package diffson.lcs

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TestPatienceBigArray extends AnyFlatSpec with Matchers {

  val lcsImpl = new Patience[Int].savedHashes

  "patience algorithm" should "be able to compute Lcs for big arrays of unique commons" in {
    val a = Stream.from(0).take(5000).toList
    val expected = (0 until a.size).map(i => (i, i + 1))
    lcsImpl.lcs(a, -1 +: a :+ -1) should be(expected)
  }
}

