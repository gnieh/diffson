package gnieh.diffson
package test

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

abstract class TestArrayDiff[JsValue, Instance <: DiffsonInstance[JsValue]](val instance: Instance) extends Properties("TestArrayDiff") {

  import instance._
  import provider._

  implicit def intSeqMarshaller: Marshaller[Seq[Int]]

  implicit def intSeqUnmarshaller: Unmarshaller[Seq[Int]]

  property("arrayDiff") = forAll {
    (a: Seq[Int], b: Seq[Int]) =>
      val p = JsonDiff.diff(a, b, false)
      p(a) == b
  }
}
