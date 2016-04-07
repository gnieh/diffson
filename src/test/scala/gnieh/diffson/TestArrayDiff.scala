package gnieh.diffson
package test

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import play.api.libs.json._

object TestArrayDiff extends Properties("TestArrayDiff") {
  property("arrayDiff") = forAll {
    (a: Seq[Int], b: Seq[Int]) =>
      val p = JsonDiff.diff(a, b, false)
      p(a) == b
  }
}
