/*
 * Copyright 2022 Lucas Satabin
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

import jsonpatch._
import jsonpointer._
import jsonmergepatch._

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

abstract class TestSerialization[Json](implicit Json: Jsony[Json])
    extends AnyFlatSpec
    with TestProtocol[Json]
    with Matchers {

  val patch = """[{
                |  "op":"replace",
                |  "path":"/a",
                |  "value":6
                |},{
                |  "op":"remove",
                |  "path":"/b"
                |},{
                |  "op":"add",
                |  "path":"/c",
                |  "value":"test2"
                |},{
                |  "op":"test",
                |  "path":"/d",
                |  "value":false
                |},{
                |  "op":"copy",
                |  "from":"/c",
                |  "path":"/e"
                |},{
                |  "op":"move",
                |  "from":"/d",
                |  "path":"/f/g"
                |}]""".stripMargin

  val mergePatch = """{
                     |  "a": 1,
                     |  "b": true
                     |}""".stripMargin

  val mergePatchValue = """"test""""

  val parsed =
    parseJson(patch)

  val mergeParsed =
    parseJson(mergePatch)

  val mergeValueParsed =
    parseJson(mergePatchValue)

  val json = JsonPatch(
    Replace(Pointer("a"), 6: Json),
    Remove(Pointer("b")),
    Add(Pointer("c"), "test2": Json),
    Test(Pointer("d"), false: Json),
    Copy(Pointer("c"), Pointer("e")),
    Move(Pointer("d"), Pointer("f", "g"))
  )

  val mergeJson: JsonMergePatch[Json] = JsonMergePatch.Object(Map("a" -> (1: Json), "b" -> (true: Json)))

  val mergeValueJson: JsonMergePatch[Json] = JsonMergePatch.Value("test": Json)

  "a patch json" should "be correctly deserialized from a Json object" in {
    parsePatch(parsed) should be(json)
  }

  "a patch object" should "be correctly serialized to a Json object" in {
    serializePatch(json) should be(parsed)
  }

  "a merge patch" should "be correctly deserialized from a Json object" in {
    parseMergePatch(mergeParsed) should be(mergeJson)
  }

  it should "be correctly deserialized from a non-object Json value" in {
    parseMergePatch(mergeValueParsed) should be(mergeValueJson)
  }

  "a merge patch object" should "be correctly serialized" in {
    serializeMergePatch(mergeJson) should be(mergeParsed)
  }

  "a non-object patch" should "be correctly serialized" in {
    serializeMergePatch(mergeValueJson) should be(mergeValueParsed)
  }

}

case class Json(a: Int, b: Boolean, c: String, d: List[Int])
