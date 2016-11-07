package gnieh.diffson
package test

import org.scalatest._

abstract class TestJsonDiff[JsValue, Instance <: DiffsonInstance[JsValue]](val instance: Instance) extends FlatSpec with Matchers {

  import instance._
  import provider._

  implicit def boolMarshaller: Marshaller[Boolean]
  implicit def intMarshaller: Marshaller[Int]
  implicit def stringMarshaller: Marshaller[String]

  import JsonDiff._

  "a diff" should "be empty if created between two equal values" in {
    val json = parseJson("true")
    diff(json, json, false) should be(JsonPatch(Nil))
  }

  it should "be a simple replacement if the two values are completely different" in {
    diff(parseJson("true"), parseJson("13"), false) should be(JsonPatch(Replace(Pointer.root, marshall(13))))
  }

  it should "contain an add operation for each added field" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 32, "new": false}""")
    val json3 = parseJson("""{"lbl": 32, "new1": false, "new2": null}""")
    val json4 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json5 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json1, json2, false) should be(JsonPatch(Add(Pointer("new"), marshall(false))))
    diff(json1, json3, false) should be(JsonPatch(Add(Pointer("new2"), JsNull), Add(Pointer("new1"), marshall(false))))
    diff(json4, json5, false) should be(JsonPatch(Add(Pointer("b", "b"), marshall(43)), Add(Pointer("c"), JsNull)))
  }

  it should "contain a remove operation for each removed field" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 32, "old": false}""")
    val json3 = parseJson("""{"lbl": 32, "old1": false, "old2": null}""")
    val json4 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json5 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json2, json1, false) should be(JsonPatch(Remove(Pointer("old"))))
    diff(json3, json1, false) should be(JsonPatch(Remove(Pointer("old2")), Remove(Pointer("old1"))))
    diff(json5, json4, false) should be(JsonPatch(Remove(Pointer("b", "b")), Remove(Pointer("c"))))
  }

  it should "correctly handle array diffs in objects" in {
    val json1 = parseJson("""{"lbl": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}""")
    val json2 = parseJson("""{"lbl": [1, 4, 5, 11, 6, 7]}""")
    diff(json1, json2, false) should be(JsonPatch(Remove(Pointer("lbl", "2")), Remove(Pointer("lbl", "1")), Add(Pointer("lbl", "3"), marshall(11)), Remove(Pointer("lbl", "8")), Remove(Pointer("lbl", "7")), Remove(Pointer("lbl", "6"))))
  }

  it should "contain a replace operation for each changed field value" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 60}""")
    val json3 = parseJson("""{"lbl": {"a": true}}""")
    val json4 = parseJson("""{"lbl": {"a": null}}""")
    diff(json1, json2, false) should be(JsonPatch(Replace(Pointer("lbl"), marshall(60))))
    diff(json1, json3, false) should be(JsonPatch(Replace(Pointer("lbl"), parseJson("""{"a": true}"""))))
    diff(json3, json4, false) should be(JsonPatch(Replace(Pointer("lbl", "a"), JsNull)))
  }

  it should "contain an add operation for each added element" in {
    val json1 = parseJson("[]")
    val json2 = parseJson("[1, 2, 3]")
    val json3 = parseJson("[1, 2, 4, 5, 6, 3]")
    diff(json1, json2, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "add", "path": "/-", "value": 1},
                   |   {"op": "add", "path": "/-", "value": 2},
                   |   {"op": "add", "path": "/-", "value": 3}
                   | ]""".stripMargin))
    diff(json2, json3, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "add", "path": "/2", "value": 4},
                   |   {"op": "add", "path": "/3", "value": 5},
                   |   {"op": "add", "path": "/4", "value": 6}
                   | ]""".stripMargin))
  }

  it should "contain a remove operation for each deleted element" in {
    val json1 = parseJson("[]")
    val json2 = parseJson("[1, 2, 3]")
    val json3 = parseJson("[1, 2, 4, 5, 6, 3]")
    diff(json2, json1, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "remove", "path": "/2"},
                   |   {"op": "remove", "path": "/1"},
                   |   {"op": "remove", "path": "/0"}
                   | ]""".stripMargin))
    diff(json3, json2, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "remove", "path": "/4"},
                   |   {"op": "remove", "path": "/3"},
                   |   {"op": "remove", "path": "/2"}
                   | ]""".stripMargin))
  }

  it should "contain a replace operation for each value that changed" in {
    val json1 = parseJson("[1, 2, 3]")
    val json2 = parseJson("[1, 2, 4]")
    val json3 = parseJson("[1, 6, 3]")
    val json4 = parseJson("""[1, {"a": 2}, 3]""")
    val json5 = parseJson("""[1, {"a": 7}, 3]""")
    diff(json1, json2, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "/2", "value": 4}
                   | ]""".stripMargin))
    diff(json1, json3, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "/1", "value": 6}
                   | ]""".stripMargin))
    diff(json4, json5, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "/1/a", "value": 7}
                   | ]""".stripMargin))
    diff(json4, json3, false) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "/1", "value": 6}
                   | ]""".stripMargin))
  }

  "applying a diff" should "be a fix point when applied to the first object used for the diff" in {
    val json1 = parseJson("""{"lbl": 32, "b": {"c": "gruik"}}""")
    val json2 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json1, json2, false)(json1) should be(json2)
  }

  "applying a diff to strings" should "provide a correct string representation" in {
    val json1 = """{
                   |  "a": 1,
                   |  "b": true,
                   |  "c": "test"
                   |}""".stripMargin
    val json2 = """{"a":6,"c":"test2","d":false}""".stripMargin
    val json3 = JsonDiff.diff(json1, json2, false)(json1)
    json3 should be(json2)
  }

  "a remembering diff" should "correctly add removed values in array diffs" in {
    val json1 = parseJson("""{"lbl": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}""")
    val json2 = parseJson("""{"lbl": [1, 4, 5, 11, 6, 7]}""")
    diff(json1, json2, true) should be(JsonPatch(
      Remove(Pointer("lbl", "2"), Some(marshall(3))),
      Remove(Pointer("lbl", "1"), Some(marshall(2))),
      Add(Pointer("lbl", "3"), marshall(11)),
      Remove(Pointer("lbl", "8"), Some(marshall(10))),
      Remove(Pointer("lbl", "7"), Some(marshall(9))),
      Remove(Pointer("lbl", "6"), Some(marshall(8)))))
  }

  it should "correctly add removed values in object diffs" in {
    val json1 = """{"a": 1, "b": true}"""
    val json2 = """{"a": 1}"""
    diff(json1, json2, true) should be(JsonPatch(Remove(Pointer("b"), Some(marshall(true)))))
  }

  it should "correctly add replaced values in object diffs" in {
    val json1 = """{"a": 1, "b": false}"""
    val json2 = """{"a": 1, "b": "test"}"""
    diff(json1, json2, true) should be(JsonPatch(Replace(Pointer("b"), marshall("test"), Some(marshall(false)))))
  }

}
