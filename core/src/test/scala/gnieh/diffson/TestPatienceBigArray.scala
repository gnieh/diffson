package gnieh.diffson
package test

import org.scalatest._

class TestPatienceBigArray extends FlatSpec with Matchers {

  val lcsImpl = new Patience[Int]

  "patience algorithm" should "be able to compute Lcs for big arrays of unique commons" in {
    val a = Stream.from(0).take(5000).toSeq
    lcsImpl.lcs(a, -1 +: a :+ -1)
  }
}

