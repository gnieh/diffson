package diffson
package jsonpatch
package test

import lcsdiff._

import cats.implicits._

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

import scala.util.Try

import scala.language.implicitConversions

abstract class TestArrayDiff[Json](implicit Json: Jsony[Json]) extends Properties("TestArrayDiff") {

  implicit val lcsalg = new lcs.Patience[Json]

  implicit def intSeqMarshaller(s: Seq[Int]): Json

  implicit def intSeqUnmarshaller(j: Json): Seq[Int]

  property("arrayDiff") = forAll {
    (a: Seq[Int], b: Seq[Int]) =>
      val p = diff(a: Json, b: Json)
      p[Try](a).get == b
  }
}
