package diffson

import jsonpatch._
import jsonpointer._
import jsonmergepatch._

import cats._
import cats.implicits._

import scala.util.Try

import scala.language.implicitConversions

trait TestProtocol[Json] {
  implicit def intSeqMarshaller(is: Seq[Int]): Json
  implicit def intSeqUnmarshaller(json: Json): Seq[Int]
  implicit def boolMarshaller(b: Boolean): Json
  implicit def intMarshaller(i: Int): Json
  implicit def stringMarshaller(s: String): Json
  implicit def jsonEq: Eq[Json]

  def parseJson(s: String): Json
  def parsePatch(s: String): JsonPatch[Json]
  def parsePatch(json: Json): JsonPatch[Json]
  def parseMergePatch(s: String): JsonMergePatch[Json]
  def parseMergePatch(json: Json): JsonMergePatch[Json]

  def serializePatch(p: JsonPatch[Json]): Json
  def serializeMergePatch(p: JsonMergePatch[Json]): Json

  def parsePointer(s: String): Pointer =
    Pointer.parse[Try](s).get

}
