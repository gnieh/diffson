package gnieh.diffson
package test
package circe

import io.circe._
import io.circe.generic.semiauto._

trait TestProtocol {
  implicit def intSeqMarshaller: Encoder[Seq[Int]] = Encoder.encodeTraversableOnce[Int, Seq]
  implicit def intSeqUnmarshaller: Decoder[Seq[Int]] = Decoder[List[Int]].map(_.toSeq)
  implicit def boolMarshaller: Encoder[Boolean] = Encoder.encodeBoolean
  implicit def boolUnmarshaller: Decoder[Boolean] = Decoder.decodeBoolean
  implicit def intMarshaller: Encoder[Int] = Encoder.encodeInt
  implicit def intUnmarshaller: Decoder[Int] = Decoder.decodeInt
  implicit def stringMarshaller: Encoder[String] = Encoder.encodeString
  implicit def testJsonMarshaller: Encoder[test.Json] = deriveEncoder[test.Json]
  implicit def testJsonUnmarshaller: Decoder[test.Json] = deriveDecoder[test.Json]
}
