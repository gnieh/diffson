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
package playJson

import jsonpatch._
import jsonmergepatch._

import play.api.libs.json._

import scala.language.implicitConversions

trait PlayJsonTestProtocol extends TestProtocol[JsValue] {
  import DiffsonProtocol._

  implicit def intSeqMarshaller(is: Seq[Int]) = Json.toJson(is)
  implicit def intSeqUnmarshaller(json: JsValue) = json.as[Seq[Int]]
  implicit def boolMarshaller(b: Boolean) = JsBoolean(b)
  implicit def intMarshaller(i: Int) = JsNumber(i)
  implicit def stringMarshaller(s: String) = JsString(s)
  implicit def jsonEq = playJsonJsony

  def parseJson(s: String): JsValue =
    Json.parse(s)
  def parsePatch(s: String): JsonPatch[JsValue] =
    Json.parse(s).as[JsonPatch[JsValue]]
  def parsePatch(json: JsValue): JsonPatch[JsValue] =
    json.as[JsonPatch[JsValue]]
  def parseMergePatch(s: String): JsonMergePatch[JsValue] =
    Json.parse(s).as[JsonMergePatch[JsValue]]
  def parseMergePatch(json: JsValue): JsonMergePatch[JsValue] =
    json.as[JsonMergePatch[JsValue]]
  def serializePatch(p: JsonPatch[JsValue]): JsValue =
    Json.toJson(p)
  def serializeMergePatch(p: JsonMergePatch[JsValue]): JsValue =
    Json.toJson(p)
}
