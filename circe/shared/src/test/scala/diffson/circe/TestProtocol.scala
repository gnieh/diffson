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
package circe

import cats.Eq
import jsonpatch._
import jsonmergepatch._
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import scala.language.implicitConversions

trait CirceTestProtocol extends TestProtocol[Json] {
  implicit def intSeqMarshaller(is: Seq[Int]): Json = is.asJson
  implicit def intSeqUnmarshaller(json: Json): Seq[Int] = json.as[Seq[Int]].fold(throw _, identity)
  implicit def boolMarshaller(b: Boolean): Json = Json.fromBoolean(b)
  implicit def intMarshaller(i: Int): Json = Json.fromInt(i)
  implicit def stringMarshaller(s: String): Json = Json.fromString(s)
  implicit def jsonEq: Eq[Json] = Json.eqJson

  def parseJson(s: String): Json =
    parse(s).fold(throw _, identity)
  def parsePatch(s: String): JsonPatch[Json] =
    parse(s).flatMap(_.as[JsonPatch[Json]]).toTry.get
  def parsePatch(json: Json): JsonPatch[Json] =
    json.as[JsonPatch[Json]].toTry.get
  def parseMergePatch(s: String): JsonMergePatch[Json] =
    parse(s).flatMap(_.as[JsonMergePatch[Json]]).toTry.get
  def parseMergePatch(json: Json): JsonMergePatch[Json] =
    json.as[JsonMergePatch[Json]].toTry.get
  def serializePatch(p: JsonPatch[Json]): Json =
    p.asJson
  def serializeMergePatch(p: JsonMergePatch[Json]): Json =
    p.asJson
}
