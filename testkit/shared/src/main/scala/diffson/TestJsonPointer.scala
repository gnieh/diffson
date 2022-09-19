/*
 * Copyright 2022 Typelevel
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
package jsonpointer

import cats.implicits._

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Try

import scala.language.implicitConversions
import org.scalatest.matchers.should.Matchers

abstract class TestJsonPointer[Json](implicit Json: Jsony[Json])
    extends AnyFlatSpec
    with Matchers
    with TestProtocol[Json] {

  "an empty string" should "be parsed as an empty pointer" in {
    parsePointer("") should be(Pointer.Root)
  }

  "the slash pointer" should "be parsed as the pointer to empty element at root" in {
    parsePointer("/") should be(Pointer(""))
  }

  "a string with a trailing forward slash" should "parse with an empty final element" in {
    parsePointer("/foo/") should be(Pointer("foo", ""))
  }

  "a pointer string with one chunk" should "be parsed as a pointer with one element" in {
    parsePointer("/test") should be(Pointer("test"))
  }

  "occurrences of ~0" should "be replaced by occurrences of ~" in {
    parsePointer("/~0/test/~0~0plop") should be(Pointer("~", "test", "~~plop"))
  }

  "occurrences of ~1" should "be replaced by occurrences of /" in {
    parsePointer("/test~1/~1/plop") should be(Pointer("test/", "/", "plop"))
  }

  "occurrences of ~" should "be directly followed by either 0 or 1" in {
    a[PointerException] should be thrownBy { parsePointer("/~") }
    a[PointerException] should be thrownBy { parsePointer("/~3") }
    a[PointerException] should be thrownBy { parsePointer("/~d") }
  }

  "a non empty pointer" should "start with a /" in {
    a[PointerException] should be thrownBy { parsePointer("test") }
  }

  "a pointer to a label" should "be evaluated to the label value if it is one level deep" in {
    parsePointer("/label").evaluate[Try, Json](parseJson("{\"label\": true}")).get should be(true: Json)
  }

  it should "be evaluated to the end label value if it is several levels deep" in {
    parsePointer("/l1/l2/l3").evaluate[Try, Json](parseJson("""{"l1": {"l2": { "l3": 17 } } }""")).get should be(
      17: Json)
  }

  it should "be evaluated to nothing if the final element is unknown" in {
    parsePointer("/lbl").evaluate[Try, Json](parseJson("{}")).get should be(Json.Null)
  }

  it should "produce an error if there is an unknown element in the middle of the pointer" in {
    a[PointerException] should be thrownBy { parsePointer("/lbl/test").evaluate[Try, Json](parseJson("{}")).get }
  }

  "a pointer to an array element" should "be evaluated to the value at the given index" in {
    parsePointer("/1").evaluate[Try, Json](parseJson("[1, 2, 3]")).get should be(2: Json)
    parsePointer("/lbl/4").evaluate[Try, Json](parseJson("{ \"lbl\": [3, 7, 5, 4, 7] }")).get should be(7: Json)
  }

  it should "produce an error if it is out of the array bounds" in {
    a[PointerException] should be thrownBy { parsePointer("/4").evaluate[Try, Json](parseJson("[1]")).get }
  }

  it should "produce an error if it is the '-' element" in {
    a[PointerException] should be thrownBy { parsePointer("/-").evaluate[Try, Json](parseJson("[1]")).get }
  }

  "a number pointer" should "be parsed as a string label if it overflows int capacity" in {
    val max = Int.MaxValue
    parsePointer(s"/$max") should be(Pointer.Root / max)
    parsePointer("/123456789012") should be(Pointer.Root / "123456789012")
  }

}
