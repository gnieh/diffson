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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import diffson.jsonpatch.JsonDiff
import diffson.lcs.Patience

abstract class TestObjectDiff[J](implicit J: Jsony[J]) extends AnyFlatSpec with Matchers {

  implicit val lcsalg: Patience[J] = new Patience[J]

  val diff = new JsonDiff[J](false, false)

  "a wide object diffed with an empty one" should "not cause stack overflows" in {
    val json1 = J.makeObject((1 to 10000).map(i => s"key$i" -> J.Null).toMap)
    val json2 = J.makeObject(Map.empty)

    diff.diff(json1, json2)
  }

  "a wide object diffed with itself" should "not cause stack overflows" in {
    val json1 = J.makeObject((1 to 10000).map(i => s"key$i" -> J.Null).toMap)

    diff.diff(json1, json1)
  }

}
