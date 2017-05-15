package gnieh.diffson
package test

import org.scalatest._

abstract class TestSimpleDiff[JsValue, Instance <: DiffsonInstance[JsValue]](val instance: Instance) extends FlatSpec with Matchers {

  import instance._
  import provider._

  implicit def boolMarshaller: Marshaller[Boolean]
  implicit def intMarshaller: Marshaller[Int]
  implicit def stringMarshaller: Marshaller[String]

  import JsonDiff._

  "a diff" should "be empty if created between two equal values" in {
    val json = parseJson("true")
    simpleDiff(json, json, false) should be(JsonPatch(Nil))
  }

  it should "be a simple replacement if the two values are completely different" in {
    simpleDiff(parseJson("true"), parseJson("13"), false) should be(JsonPatch(Replace(Pointer.root, marshall(13))))
  }

  it should "contain an add operation for each added field" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 32, "new": false}""")
    val json3 = parseJson("""{"lbl": 32, "new1": false, "new2": null}""")
    val json4 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json5 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    simpleDiff(json1, json2, false) should be(JsonPatch(Add(Pointer("new"), marshall(false))))
    simpleDiff(json1, json3, false) should be(JsonPatch(Add(Pointer("new2"), JsNull), Add(Pointer("new1"), marshall(false))))
    simpleDiff(json4, json5, false) should be(JsonPatch(Add(Pointer("b", "b"), marshall(43)), Add(Pointer("c"), JsNull)))
  }

  it should "contain a remove operation for each removed field" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 32, "old": false}""")
    val json3 = parseJson("""{"lbl": 32, "old1": false, "old2": null}""")
    val json4 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json5 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    simpleDiff(json2, json1, false) should be(JsonPatch(Remove(Pointer("old"))))
    simpleDiff(json3, json1, false) should be(JsonPatch(Remove(Pointer("old2")), Remove(Pointer("old1"))))
    simpleDiff(json5, json4, false) should be(JsonPatch(Remove(Pointer("b", "b")), Remove(Pointer("c"))))
  }

  it should "correctly handle array diffs in objects (i.e. just replaced)" in {
    val json1 = parseJson("""{"lbl": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}""")
    val json2 = parseJson("""{"lbl": [1, 4, 5, 11, 6, 7]}""")
    simpleDiff(json1, json2, false) should be(JsonPatch(Replace(Pointer("lbl"), JsArray(Vector(marshall(1), marshall(4), marshall(5), marshall(11), marshall(6), marshall(7))))))
  }

  it should "contain a replace operation for each changed field value" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 60}""")
    val json3 = parseJson("""{"lbl": {"a": true}}""")
    val json4 = parseJson("""{"lbl": {"a": null}}""")
    simpleDiff(json1, json2, false) should be(JsonPatch(Replace(Pointer("lbl"), marshall(60))))
    simpleDiff(json1, json3, false) should be(JsonPatch(Replace(Pointer("lbl"), parseJson("""{"a": true}"""))))
    simpleDiff(json3, json4, false) should be(JsonPatch(Replace(Pointer("lbl", "a"), JsNull)))
  }

  it should "contain a replaced operation for the changed array (additions)" in {
    val json1 = parseJson("[]")
    val json2 = parseJson("[1, 2, 3]")
    val json3 = parseJson("[1, 2, 4, 5, 6, 3]")
    simpleDiff(json1, json2, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "", "value": [1, 2, 3]}
                   | ]""".stripMargin))
    simpleDiff(json2, json3, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "", "value": [1, 2, 4, 5, 6, 3]}
                   | ]""".stripMargin))
  }

  it should "contain a replaced operation for the changed array (deletions)" in {
    val json1 = parseJson("[]")
    val json2 = parseJson("[1, 2, 3]")
    val json3 = parseJson("[1, 2, 4, 5, 6, 3]")
    simpleDiff(json2, json1, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "", "value": []}
                   | ]""".stripMargin))
    simpleDiff(json3, json2, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "", "value": [1, 2, 3]}
                   | ]""".stripMargin))
  }

  it should "contain a replace operation for the entire array if at least one element in it changed" in {
    val json1 = parseJson("[1, 2, 3]")
    val json2 = parseJson("[1, 2, 4]")
    val json3 = parseJson("[1, 6, 3]")
    val json4 = parseJson("""[1, {"a": 2}, 3]""")
    val json5 = parseJson("""[1, {"a": 7}, 3]""")
    simpleDiff(json1, json2, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "", "value": [1, 2, 4]}
                   | ]""".stripMargin))
    simpleDiff(json1, json3, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "", "value": [1, 6, 3]}
                   | ]""".stripMargin))
    simpleDiff(json4, json5, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "", "value": [1, {"a": 7}, 3]}
                   | ]""".stripMargin))
    simpleDiff(json4, json3, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "", "value": [1, 6, 3]}
                   | ]""".stripMargin))
  }

  "applying a diff" should "be a fix point when applied to the first object used for the diff" in {
    val json1 = parseJson("""{"lbl": 32, "b": {"c": "gruik"}}""")
    val json2 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    simpleDiff(json1, json2, false)(json1) should be(json2)
  }

  "applying a diff to strings" should "provide a correct string representation" in {
    val json1 = """{
                   |  "a": 1,
                   |  "b": true,
                   |  "c": "test"
                   |}""".stripMargin
    val json2 = """{"a":6,"c":"test2","d":false}""".stripMargin
    val json3 = JsonDiff.simpleDiff(json1, json2, false)(json1)
    json3 should be(json2)
  }

  "a remembering diff" should "correctly remember old value array" in {
    val json1 = parseJson("""{"lbl": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}""")
    val json2 = parseJson("""{"lbl": [1, 4, 5, 11, 6, 7]}""")
    simpleDiff(json1, json2, true) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "/lbl", "value": [1, 4, 5, 11, 6, 7], "old": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}
                   | ]""".stripMargin))
  }

  it should "correctly add removed values in object diffs" in {
    val json1 = """{"a": 1, "b": true}"""
    val json2 = """{"a": 1}"""
    simpleDiff(json1, json2, true) should be(JsonPatch(Remove(Pointer("b"), Some(marshall(true)))))
  }

  it should "correctly add replaced values in object diffs" in {
    val json1 = """{"a": 1, "b": false}"""
    val json2 = """{"a": 1, "b": "test"}"""
    simpleDiff(json1, json2, true) should be(JsonPatch(Replace(Pointer("b"), marshall("test"), Some(marshall(false)))))
  }

}
