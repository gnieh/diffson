package diffson
package test

import playJson._
import jsonpatch._
import jsonmergepatch._

import play.api.libs.json._

import scala.language.implicitConversions

trait TestProtocol {
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
}
