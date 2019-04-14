package diffson
package circe

import jsonpatch._
import jsonmergepatch._

import cats.implicits._

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._

import scala.language.implicitConversions

trait CirceTestProtocol extends TestProtocol[Json] {
  implicit def intSeqMarshaller(is: Seq[Int]) = is.asJson
  implicit def intSeqUnmarshaller(json: Json) = json.as[Seq[Int]].right.get
  implicit def boolMarshaller(b: Boolean) = Json.fromBoolean(b)
  implicit def intMarshaller(i: Int) = Json.fromInt(i)
  implicit def stringMarshaller(s: String) = Json.fromString(s)
  implicit def jsonEq = Json.eqJson

  def parseJson(s: String): Json =
    parse(s).right.get
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
