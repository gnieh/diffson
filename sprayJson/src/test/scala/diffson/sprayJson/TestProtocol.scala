package diffson
package sprayJson

import jsonpatch._
import jsonmergepatch._

import spray.json._

import scala.language.implicitConversions

trait SprayJsonTestProtocol extends TestProtocol[JsValue] {
  import DiffsonProtocol._

  implicit def intSeqMarshaller(is: Seq[Int]) = is.toJson
  implicit def intSeqUnmarshaller(json: JsValue) = json.convertTo[Seq[Int]]
  implicit def boolMarshaller(b: Boolean) = JsBoolean(b)
  implicit def intMarshaller(i: Int) = JsNumber(i)
  implicit def stringMarshaller(s: String) = JsString(s)
  implicit def jsonEq = sprayJsonJsony

  def parseJson(s: String): JsValue =
    s.parseJson
  def parsePatch(s: String): JsonPatch[JsValue] =
    s.parseJson.convertTo[JsonPatch[JsValue]]
  def parsePatch(json: JsValue): JsonPatch[JsValue] =
    json.convertTo[JsonPatch[JsValue]]
  def parseMergePatch(s: String): JsonMergePatch[JsValue] =
    s.parseJson.convertTo[JsonMergePatch[JsValue]]
  def parseMergePatch(json: JsValue): JsonMergePatch[JsValue] =
    json.convertTo[JsonMergePatch[JsValue]]
  def serializePatch(p: JsonPatch[JsValue]): JsValue =
    p.toJson
  def serializeMergePatch(p: JsonMergePatch[JsValue]): JsValue =
    p.toJson
}
