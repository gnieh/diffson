package gnieh.diffson
package test
package sprayJson

import spray.json._

trait TestProtocol {
  import DefaultJsonProtocol._
  implicit def intSeqMarshaller: JsonWriter[Seq[Int]] = seqFormat[Int]
  implicit def intSeqUnmarshaller: JsonReader[Seq[Int]] = seqFormat[Int]
  implicit def boolMarshaller: JsonWriter[Boolean] = BooleanJsonFormat
  implicit def boolUnmarshaller: JsonReader[Boolean] = BooleanJsonFormat
  implicit def intMarshaller: JsonWriter[Int] = IntJsonFormat
  implicit def intUnmarshaller: JsonReader[Int] = IntJsonFormat
  implicit def stringMarshaller: JsonWriter[String] = StringJsonFormat
  implicit def testJsonMarshaller: JsonWriter[Json] = jsonFormat4(Json)
  implicit def testJsonUnmarshaller: JsonReader[Json] = jsonFormat4(Json)
}
