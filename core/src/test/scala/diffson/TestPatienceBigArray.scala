package diffson.lcs

import cats.implicits._

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TestPatienceBigArray extends AnyFlatSpec with Matchers {

  val lcsImpl = new Patience[Int].savedHashes

  "patience algorithm" should "be able to compute Lcs for big arrays of unique commons" in {
    val a = (0 until 5000).toList
    val expected = a.map(i => (i, i + 1))
    lcsImpl.lcs(a, -1 +: a :+ -1) should be(expected)
  }
}

