package diffson
package jsonpatch

import lcsdiff._

import cats.implicits._

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

import scala.util.Try

import scala.language.implicitConversions

abstract class TestArrayDiff[Json](implicit Json: Jsony[Json]) extends Properties("TestArrayDiff") with TestProtocol[Json] {

  implicit val lcsalg = new lcs.Patience[Json]

  property("arrayDiff") = forAll {
    (a: Seq[Int], b: Seq[Int]) =>
      val p = diff(a: Json, b: Json)
      p[Try](a).get == (b: Json)
  }
}
