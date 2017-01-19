package gnieh.diffson
package test
package playJson

import play.api.libs.json._
import play.api.libs.functional.syntax._

trait TestProtocol {
  implicit def intSeqMarshaller: Writes[Seq[Int]] = Writes.seq[Int]
  implicit def intSeqUnmarshaller: Reads[Seq[Int]] = Reads.seq[Int]
  implicit def boolMarshaller: Writes[Boolean] = Writes.BooleanWrites
  implicit def boolUnmarshaller: Reads[Boolean] = Reads.BooleanReads
  implicit def intMarshaller: Writes[Int] = Writes.IntWrites
  implicit def intUnmarshaller: Reads[Int] = Reads.IntReads
  implicit def stringMarshaller: Writes[String] = Writes.StringWrites
  implicit def testJsonMarshaller: Writes[test.Json] = (
    (JsPath \ "a").write[Int] and
    (JsPath \ "b").write[Boolean] and
    (JsPath \ "c").write[String] and
    (JsPath \ "d").write[List[Int]])(unlift(test.Json.unapply))
  implicit def testJsonUnmarshaller: Reads[test.Json] = (
    (JsPath \ "a").read[Int] and
    (JsPath \ "b").read[Boolean] and
    (JsPath \ "c").read[String] and
    (JsPath \ "d").read[List[Int]])(test.Json.apply _)
}
