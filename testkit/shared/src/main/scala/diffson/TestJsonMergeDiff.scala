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
package jsonmergepatch

import org.scalatest.flatspec.AnyFlatSpec

import org.scalatest.matchers.should.Matchers

abstract class TestJsonMergeDiff[Json](implicit Json: Jsony[Json])
    extends AnyFlatSpec
    with Matchers
    with TestProtocol[Json] {

  "a diff" should "be empty if created between two equal objects" in {
    val json = parseJson("""{"a": true}""")
    diff(json, json) should be(JsonMergePatch.Object(Map()))
  }

  it should "be a simple replacement if the two values are completely different" in {
    diff(parseJson("true"), parseJson("13")) should be(JsonMergePatch.Value(13: Json))
  }

  it should "be generated correctly for nested structures" in {
    val json1 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json2 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}}""")
    diff(json1, json2) should be(JsonMergePatch.Object(Map("b" -> JsObject(Map("b" -> (43: Json))))))
  }

}
