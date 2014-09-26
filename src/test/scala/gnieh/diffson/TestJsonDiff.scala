package gnieh.diffson
package test

import org.scalatest._

import spray.json._

class TestJsonDiff extends FlatSpec with ShouldMatchers {

  val pointer = JsonPointer

  import JsonDiff._

  "a diff" should "be empty if created between two equal values" in {
    val json = JsonParser("true")
    diff(json, json) should be(JsonPatch(Nil))
  }

  it should "be a simple replacement if the two values are completely different" in {
    diff(JsonParser("true"), JsonParser("13")) should be(JsonPatch(Replace(Nil, JsNumber(13))))
  }

  it should "contain an add operation for each added field" in {
    val json1 = JsonParser("""{"lbl": 32}""")
    val json2 = JsonParser("""{"lbl": 32, "new": false}""")
    val json3 = JsonParser("""{"lbl": 32, "new1": false, "new2": null}""")
    val json4 = JsonParser("""{"a": 3, "b": {"a": true }}""")
    val json5 = JsonParser("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json1, json2) should be(JsonPatch(Add("new", JsBoolean(false))))
    diff(json1, json3) should be(JsonPatch(Add("new1", JsBoolean(false)), Add("new2", JsNull)))
    diff(json4, json5) should be(JsonPatch(Add("b" :: "b", JsNumber(43)), Add("c", JsNull)))
  }

  it should "contain a remove operation for each removed field" in {
    val json1 = JsonParser("""{"lbl": 32}""")
    val json2 = JsonParser("""{"lbl": 32, "old": false}""")
    val json3 = JsonParser("""{"lbl": 32, "old1": false, "old2": null}""")
    val json4 = JsonParser("""{"a": 3, "b": {"a": true }}""")
    val json5 = JsonParser("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json2, json1) should be(JsonPatch(Remove("old")))
    diff(json3, json1) should be(JsonPatch(Remove("old1"), Remove("old2")))
    diff(json5, json4) should be(JsonPatch(Remove("b" :: "b"), Remove("c")))
  }

  it should "contain a replace operation for each changed field value" in {
    val json1 = JsonParser("""{"lbl": 32}""")
    val json2 = JsonParser("""{"lbl": 60}""")
    val json3 = JsonParser("""{"lbl": {"a": true}}""")
    val json4 = JsonParser("""{"lbl": {"a": null}}""")
    diff(json1, json2) should be(JsonPatch(Replace("lbl", JsNumber(60))))
    diff(json1, json3) should be(JsonPatch(Replace("lbl", JsonParser("""{"a": true}"""))))
    diff(json3, json4) should be(JsonPatch(Replace("lbl" :: "a", JsNull)))
  }

  it should "contain an add operation for each added element" in {
    val json1 = JsonParser("[]")
    val json2 = JsonParser("[1, 2, 3]")
    val json3 = JsonParser("[1, 2, 4, 5, 6, 3]")
    diff(json1, json2) should be(
      JsonPatch.parse("""[
                   |   {"op": "add", "path": "/-", "value": 1},
                   |   {"op": "add", "path": "/-", "value": 2},
                   |   {"op": "add", "path": "/-", "value": 3}
                   | ]""".stripMargin))
    diff(json2, json3) should be(
      JsonPatch.parse("""[
                   |   {"op": "add", "path": "/2", "value": 4},
                   |   {"op": "add", "path": "/3", "value": 5},
                   |   {"op": "add", "path": "/4", "value": 6}
                   | ]""".stripMargin))
  }

  it should "contain a remove operation for each deleted element" in {
    val json1 = JsonParser("[]")
    val json2 = JsonParser("[1, 2, 3]")
    val json3 = JsonParser("[1, 2, 4, 5, 6, 3]")
    diff(json2, json1) should be(
      JsonPatch.parse("""[
                   |   {"op": "remove", "path": "/2"},
                   |   {"op": "remove", "path": "/1"},
                   |   {"op": "remove", "path": "/0"}
                   | ]""".stripMargin))
    diff(json3, json2) should be(
      JsonPatch.parse("""[
                   |   {"op": "remove", "path": "/4"},
                   |   {"op": "remove", "path": "/3"},
                   |   {"op": "remove", "path": "/2"}
                   | ]""".stripMargin))
  }

  it should "contain a replace operation for each value that changed" in {
    val json1 = JsonParser("[1, 2, 3]")
    val json2 = JsonParser("[1, 2, 4]")
    val json3 = JsonParser("[1, 6, 3]")
    val json4 = JsonParser("""[1, {"a": 2}, 3]""")
    val json5 = JsonParser("""[1, {"a": 7}, 3]""")
    diff(json1, json2) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "/2", "value": 4}
                   | ]""".stripMargin))
    diff(json1, json3) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "/1", "value": 6}
                   | ]""".stripMargin))
    diff(json4, json5) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "/1/a", "value": 7}
                   | ]""".stripMargin))
    diff(json4, json3) should be(
      JsonPatch.parse("""[
                   |   {"op": "replace", "path": "/1", "value": 6}
                   | ]""".stripMargin))
  }

  "applying a diff" should "be a fix point when applied to the first object used for the diff" in {
    val json1 = JsonParser("""{"lbl": 32, "b": {"c": "gruik"}}""")
    val json2 = JsonParser("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json1, json2)(json1) should be(json2)
  }

}
