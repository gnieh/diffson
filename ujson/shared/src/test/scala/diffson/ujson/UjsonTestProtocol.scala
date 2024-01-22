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
package ujson

import _root_.ujson.{Arr, Bool, Str, Value}
import cats.Eq
import diffson.jsonmergepatch._
import diffson.jsonpatch._
import upickle.default._

trait UjsonTestProtocol extends TestProtocol[Value] {

  implicit def intSeqMarshaller(is: Seq[Int]): Value = Arr.from(is)
  implicit def intSeqUnmarshaller(json: Value): Seq[Int] =
    json.arr.toSeq.map(_.num.toInt)
  implicit def boolMarshaller(b: Boolean): Value = Bool(b)
  implicit def intMarshaller(i: Int): Value = Value.JsonableInt(i)
  implicit def stringMarshaller(s: String): Value = Str(s)
  implicit def jsonEq: Eq[Value] = (l: Value, r: Value) => l == r

  def parseJson(s: String): Value = {
    val readable = _root_.ujson.Readable.fromString(s)
    val jsonRep = _root_.ujson.read(readable)
    read[Value](jsonRep)
  }

  def parsePatch(s: String): JsonPatch[Value] = {
    read[JsonPatch[Value]](parseJson(s))
  }

  def parsePatch(json: Value): JsonPatch[Value] = read[JsonPatch[Value]](json)
  def parseMergePatch(s: String): JsonMergePatch[Value] = {
    val readable = _root_.ujson.Readable.fromString(s)
    val jsonRep = _root_.ujson.read(readable)
    read[JsonMergePatch[Value]](jsonRep)
  }

  def parseMergePatch(json: Value): JsonMergePatch[Value] =
    read[JsonMergePatch[Value]](json)

  def serializePatch(p: JsonPatch[Value]): Value = {
    val jsonRep = write[JsonPatch[Value]](p)
    parseJson(jsonRep)
  }

  def serializeMergePatch(p: JsonMergePatch[Value]): Value = p.toJson
}
