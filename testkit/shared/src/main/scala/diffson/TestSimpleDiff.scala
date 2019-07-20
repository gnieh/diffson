package diffson
package jsonpatch

import simplediff._
import jsonpointer._

import cats._
import cats.implicits._

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Try

import scala.language.implicitConversions

abstract class TestSimpleDiff[Json](implicit val Json: Jsony[Json]) extends AnyFlatSpec with Matchers with TestProtocol[Json] {

  "a diff" should "be empty if created between two equal values" in {
    val json = parseJson("true")
    diff(json, json) should be(JsonPatch[Json](Nil))
  }

  it should "be a simple replacement if the two values are completely different" in {
    diff(parseJson("true"), parseJson("13")) should be(JsonPatch[Json](Replace(Pointer.Root, 13: Json)))
  }

  it should "contain an add operation for each added field" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 32, "new": false}""")
    val json3 = parseJson("""{"lbl": 32, "new1": false, "new2": null}""")
    val json4 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json5 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json1, json2) should be(JsonPatch[Json](Add(Pointer("new"), false: Json)))
    diff(json1, json3) should be(JsonPatch[Json](Add(Pointer("new2"), Json.Null), Add(Pointer("new1"), false: Json)))
    diff(json4, json5) should be(JsonPatch[Json](Add(Pointer("b", "b"), 43: Json), Add(Pointer("c"), Json.Null)))
  }

  it should "contain a remove operation for each removed field" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 32, "old": false}""")
    val json3 = parseJson("""{"lbl": 32, "old1": false, "old2": null}""")
    val json4 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json5 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json2, json1) should be(JsonPatch[Json](Remove(Pointer("old"))))
    diff(json3, json1) should be(JsonPatch[Json](Remove(Pointer("old2")), Remove(Pointer("old1"))))
    diff(json5, json4) should be(JsonPatch[Json](Remove(Pointer("b", "b")), Remove(Pointer("c"))))
  }

  it should "correctly handle array diffs in objects (i.e. just replaced)" in {
    val json1 = parseJson("""{"lbl": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}""")
    val json2 = parseJson("""{"lbl": [1, 4, 5, 11, 6, 7]}""")
    diff(json1, json2) should be(JsonPatch[Json](Replace(Pointer("lbl"), JsArray(Vector[Json](1, 4, 5, 11, 6, 7)))))
  }

  it should "contain a replace operation for each changed field value" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 60}""")
    val json3 = parseJson("""{"lbl": {"a": true}}""")
    val json4 = parseJson("""{"lbl": {"a": null}}""")
    diff(json1, json2) should be(JsonPatch[Json](Replace(Pointer("lbl"), 60: Json)))
    diff(json1, json3) should be(JsonPatch[Json](Replace(Pointer("lbl"), parseJson("""{"a": true}"""))))
    diff(json3, json4) should be(JsonPatch[Json](Replace(Pointer("lbl", "a"), Json.Null)))
  }

  it should "contain a replaced operation for the changed array (additions)" in {
    val json1 = parseJson("[]")
    val json2 = parseJson("[1, 2, 3]")
    val json3 = parseJson("[1, 2, 4, 5, 6, 3]")
    diff(json1, json2) should be(
      parsePatch("""[
                   |   {"op": "replace", "path": "", "value": [1, 2, 3]}
                   | ]""".stripMargin))
    diff(json2, json3) should be(
      parsePatch("""[
                   |   {"op": "replace", "path": "", "value": [1, 2, 4, 5, 6, 3]}
                   | ]""".stripMargin))
  }

  it should "contain a replaced operation for the changed array (deletions)" in {
    val json1 = parseJson("[]")
    val json2 = parseJson("[1, 2, 3]")
    val json3 = parseJson("[1, 2, 4, 5, 6, 3]")
    diff(json2, json1) should be(
      parsePatch("""[
                   |   {"op": "replace", "path": "", "value": []}
                   | ]""".stripMargin))
    diff(json3, json2) should be(
      parsePatch("""[
                   |   {"op": "replace", "path": "", "value": [1, 2, 3]}
                   | ]""".stripMargin))
  }

  it should "contain a replace operation for the entire array if at least one element in it changed" in {
    val json1 = parseJson("[1, 2, 3]")
    val json2 = parseJson("[1, 2, 4]")
    val json3 = parseJson("[1, 6, 3]")
    val json4 = parseJson("""[1, {"a": 2}, 3]""")
    val json5 = parseJson("""[1, {"a": 7}, 3]""")
    diff(json1, json2) should be(
      parsePatch("""[
                   |   {"op": "replace", "path": "", "value": [1, 2, 4]}
                   | ]""".stripMargin))
    diff(json1, json3) should be(
      parsePatch("""[
                   |   {"op": "replace", "path": "", "value": [1, 6, 3]}
                   | ]""".stripMargin))
    diff(json4, json5) should be(
      parsePatch("""[
                   |   {"op": "replace", "path": "", "value": [1, {"a": 7}, 3]}
                   | ]""".stripMargin))
    diff(json4, json3) should be(
      parsePatch("""[
                   |   {"op": "replace", "path": "", "value": [1, 6, 3]}
                   | ]""".stripMargin))
  }

  "applying a diff" should "be a fix point when applied to the first object used for the diff" in {
    val json1 = parseJson("""{"lbl": 32, "b": {"c": "gruik"}}""")
    val json2 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json1, json2).apply[Try](json1).get should be(json2)
  }

  "applying a diff to strings" should "provide a correct string representation" in {
    val json1 = parseJson("""{
                   |  "a": 1,
                   |  "b": true,
                   |  "c": "test"
                   |}""".stripMargin)
    val json2 = parseJson("""{"a":6,"c":"test2","d":false}""".stripMargin)
    val json3 = diff(json1, json2).apply[Try](json1).get
    json3 should be(json2)
  }

  it should "correctly add removed values in object diffs" in {
    val json1 = parseJson("""{"a": 1, "b": true}""")
    val json2 = parseJson("""{"a": 1}""")
    diff(json1, json2) should be(JsonPatch[Json](Remove(Pointer("b"))))
  }

  it should "correctly add replaced values in object diffs" in {
    val json1 = parseJson("""{"a": 1, "b": false}""")
    val json2 = parseJson("""{"a": 1, "b": "test"}""")
    diff(json1, json2) should be(JsonPatch[Json](Replace(Pointer("b"), "test": Json)))
  }

}
