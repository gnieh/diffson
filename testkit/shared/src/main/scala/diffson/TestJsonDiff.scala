/*
 * Copyright 2024 Diffson Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package diffson
package jsonpatch

import lcsdiff._
import jsonpointer._
import cats.implicits._
import diffson.lcs.Patience
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Try
import org.scalatest.matchers.should.Matchers

abstract class TestJsonDiff[Json](implicit Json: Jsony[Json])
    extends AnyFlatSpec
    with Matchers
    with TestProtocol[Json] {

  implicit val lcsalg: Patience[Json] = new lcs.Patience[Json]

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
    diff(json1, json2).ops should contain theSameElementsAs List(Add(Pointer("new"), false: Json))
    diff(json1, json3).ops should contain theSameElementsAs List(Add(Pointer("new2"), Json.Null),
                                                                 Add(Pointer("new1"), false: Json))
    diff(json4, json5).ops should contain theSameElementsAs List(Add(Pointer("b", "b"), 43: Json),
                                                                 Add(Pointer("c"), Json.Null))
  }

  it should "contain a remove operation for each removed field" in {
    val json1 = parseJson("""{"lbl": 32}""")
    val json2 = parseJson("""{"lbl": 32, "old": false}""")
    val json3 = parseJson("""{"lbl": 32, "old1": false, "old2": null}""")
    val json4 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json5 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}, "c": null}""")
    diff(json2, json1).ops should contain theSameElementsAs List(Remove(Pointer("old")))
    diff(json3, json1).ops should contain theSameElementsAs List(Remove(Pointer("old2")), Remove(Pointer("old1")))
    diff(json5, json4).ops should contain theSameElementsAs List(Remove(Pointer("b", "b")), Remove(Pointer("c")))
  }

  it should "correctly handle array diffs in objects" in {
    val json1 = parseJson("""{"lbl": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}""")
    val json2 = parseJson("""{"lbl": [1, 4, 5, 11, 6, 7]}""")
    diff(json1, json2) should be(
      JsonPatch[Json](
        Remove(Pointer("lbl", "2")),
        Remove(Pointer("lbl", "1")),
        Add(Pointer("lbl", "3"), 11: Json),
        Remove(Pointer("lbl", "8")),
        Remove(Pointer("lbl", "7")),
        Remove(Pointer("lbl", "6"))
      ))
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

  it should "contain an add operation for each added element" in {
    val json1 = parseJson("[]")
    val json2 = parseJson("[1, 2, 3]")
    val json3 = parseJson("[1, 2, 4, 5, 6, 3]")
    diff(json1, json2) should be(parsePatch("""[
                   |   {"op": "add", "path": "/-", "value": 1},
                   |   {"op": "add", "path": "/-", "value": 2},
                   |   {"op": "add", "path": "/-", "value": 3}
                   | ]""".stripMargin))
    diff(json2, json3) should be(parsePatch("""[
                   |   {"op": "add", "path": "/2", "value": 4},
                   |   {"op": "add", "path": "/3", "value": 5},
                   |   {"op": "add", "path": "/4", "value": 6}
                   | ]""".stripMargin))
  }

  it should "contain a remove operation for each deleted element" in {
    val json1 = parseJson("[]")
    val json2 = parseJson("[1, 2, 3]")
    val json3 = parseJson("[1, 2, 4, 5, 6, 3]")
    diff(json2, json1) should be(parsePatch("""[
                   |   {"op": "remove", "path": "/2"},
                   |   {"op": "remove", "path": "/1"},
                   |   {"op": "remove", "path": "/0"}
                   | ]""".stripMargin))
    diff(json3, json2) should be(parsePatch("""[
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
    diff(json1, json2) should be(parsePatch("""[
                   |   {"op": "replace", "path": "/2", "value": 4}
                   | ]""".stripMargin))
    diff(json1, json3) should be(parsePatch("""[
                   |   {"op": "replace", "path": "/1", "value": 6}
                   | ]""".stripMargin))
    diff(json4, json5) should be(parsePatch("""[
                   |   {"op": "replace", "path": "/1/a", "value": 7}
                   | ]""".stripMargin))
    diff(json4, json3) should be(parsePatch("""[
                   |   {"op": "replace", "path": "/1", "value": 6}
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

  "a remembering diff" should "correctly add removed values in array diffs" in {
    val json1 = parseJson("""{"lbl": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}""")
    val json2 = parseJson("""{"lbl": [1, 4, 5, 11, 6, 7]}""")
    import remembering._
    diff(json1, json2) should be(
      JsonPatch(
        Remove(Pointer("lbl", "2"), Some(3: Json)),
        Remove(Pointer("lbl", "1"), Some(2: Json)),
        Add(Pointer("lbl", "3"), 11: Json),
        Remove(Pointer("lbl", "8"), Some(10: Json)),
        Remove(Pointer("lbl", "7"), Some(9: Json)),
        Remove(Pointer("lbl", "6"), Some(8: Json))
      ))
  }

  it should "correctly add removed values in object diffs" in {
    val json1 = parseJson("""{"a": 1, "b": true}""")
    val json2 = parseJson("""{"a": 1}""")
    import remembering._
    diff(json1, json2) should be(JsonPatch(Remove(Pointer("b"), Some(true: Json))))
  }

  it should "correctly add replaced values in object diffs" in {
    val json1 = parseJson("""{"a": 1, "b": false}""")
    val json2 = parseJson("""{"a": 1, "b": "test"}""")
    import remembering._
    diff(json1, json2) should be(JsonPatch(Replace(Pointer("b"), "test": Json, Some(false: Json))))
  }

}
