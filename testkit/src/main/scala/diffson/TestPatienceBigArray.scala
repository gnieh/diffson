package diffson.lcs

import cats.implicits._

import org.scalatest._

class TestPatienceBigArray extends FlatSpec with Matchers {

  val lcsImpl = new Patience[Int].savedHashes

  "patience algorithm" should "be able to compute Lcs for big arrays of unique commons" in {
    val a = Stream.from(0).take(5000).toList
    val expected = (0 until a.size).map(i => (i, i + 1))
    lcsImpl.lcs(a, -1 +: a :+ -1) should be(expected)
  }
}

