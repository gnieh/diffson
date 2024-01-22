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

import cats.implicits._

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Try
import org.scalatest.matchers.should.Matchers

abstract class TestJsonMergePatch[Json](implicit Json: Jsony[Json])
    extends AnyFlatSpec
    with Matchers
    with TestProtocol[Json] {

  val samples = List(
    ("""{"a":"b"}""", """{"a":"c"}""", """{"a":"c"}"""),
    ("""{"a":"b"}""", """{"b":"c"}""", """{"a":"b","b":"c"}"""),
    ("""{"a":"b"}""", """{"a":null}""", """{}"""),
    ("""{"a":"b","b":"c"}""", """{"a":null}""", """{"b":"c"}"""),
    ("""{"a":["b"]}""", """{"a":"c"}""", """{"a":"c"}"""),
    ("""{"a":"c"}""", """{"a":["b"]}""", """{"a":["b"]}"""),
    ("""{"a": {"b": "c"}}""", """{"a": {"b": "d","c": null}}""", """{"a": {"b": "d"}}"""),
    ("""{"a": [{"b":"c"}]}""", """{"a": [1]}""", """{"a": [1]}"""),
    ("""["a","b"]""", """["c","d"]""", """["c","d"]"""),
    ("""{"a":"b"}""", """["c"]""", """["c"]"""),
    ("""{"a":"foo"}""", """null""", """null"""),
    ("""{"a":"foo"}""", """"bar"""", """"bar""""),
    ("""{"e":null}""", """{"a":1}""", """{"e":null,"a":1}"""),
    ("""[1,2]""", """{"a":"b","c":null}""", """{"a":"b"}"""),
    ("""{}""", """{"a":{"bb":{"ccc": null}}}""", """{"a":{"bb":{}}}""")
  )

  for ((original, patch, result) <- samples)
    s"patching $original with $patch" should s"result in $result" in {
      val orig = parseJson(original)
      val p = parseMergePatch(patch)
      val res = parseJson(result)
      p[Try](orig).get should be(res)
    }

}
