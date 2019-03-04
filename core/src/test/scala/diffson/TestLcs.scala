package diffson.lcs
package test

import org.scalatest._

abstract class TestLcs extends FlatSpec with Matchers {

  val lcsImpl: Lcs[Char]

  "the lcs of an empty sequence and another sequence" should "be the empty sequence" in {

    val lcs1 = lcsImpl.lcs("".toList, "abcdef".toList)

    lcs1 should be(Nil)

    val lcs2 = lcsImpl.lcs("abcdef".toList, "".toList)

    lcs2 should be(Nil)

  }

  "the lcs of two equal strings" should "be the strings" in {

    val str = "abcde".toList
    val lcs = lcsImpl.lcs(str, str)
    lcs should be(List(0 -> 0, 1 -> 1, 2 -> 2, 3 -> 3, 4 -> 4))

  }

  "the lcs of a string and a prefix" should "be the prefix" in {

    val str = "abcdef".toList
    val prefix = "abc".toList
    val lcs1 = lcsImpl.lcs(str, prefix)

    lcs1 should be(List(0 -> 0, 1 -> 1, 2 -> 2))

    val lcs2 = lcsImpl.lcs(prefix, str)

    lcs2 should be(List(0 -> 0, 1 -> 1, 2 -> 2))

  }

  "the lcs of two strings with no common characters " should "be empty" in {

    val str1 = "abcdef".toList
    val str2 = "ghijkl".toList

    val lcs = lcsImpl.lcs(str1, str2)

    lcs should be(Nil)
  }

  "the lcs of two strings" should "be correctly computed when one is in the middle of the other one" in {

    val str1 = "abcdefgh".toList
    val str2 = "bdeg".toList

    val lcs = lcsImpl.lcs(str1, str2)

    lcs should be(List(1 -> 0, 3 -> 1, 4 -> 2, 6 -> 3))
  }

  it should "be correctly computed with a repeated character in common" in {

    val str1 = "abcbdbebf".toList
    val str2 = "bbbb".toList

    val lcs = lcsImpl.lcs(str1, str2)

    lcs should be(List(1 -> 0, 3 -> 1, 5 -> 2, 7 -> 3))
  }

  it should "be correctly computed with non unique characters" in {

    val str1 = "abcdabcd".toList
    val str2 = "gabhakbf".toList

    val lcs = lcsImpl.lcs(str1, str2)

    lcs should be(List(0 -> 1, 1 -> 2, 4 -> 4, 5 -> 6))
  }

  it should "be correctly computed when both sequences have a common prefix and suffix" in {

    val str1 = "abctotodef".toList
    val str2 = "abctatatadef".toList

    val lcs = lcsImpl.lcs(str1, str2)

    lcs should be(List(0 -> 0, 1 -> 1, 2 -> 2, 3 -> 3, 5 -> 5, 7 -> 9, 8 -> 10, 9 -> 11))

  }

}
