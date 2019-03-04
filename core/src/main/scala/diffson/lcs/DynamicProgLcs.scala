package diffson.lcs

import cats.Eq
import cats.implicits._

import scala.annotation.tailrec

/** Implementation of the LCS using dynamic programming.
 *
 *  @author Lucas Satabin
 */
class DynamicProgLcs[T: Eq] extends Lcs[T] {

  def lcs(s1: List[T], s2: List[T], low1: Int, high1: Int, low2: Int, high2: Int): List[(Int, Int)] = {
    val seq1 = s1.slice(low1, high1)
    val seq2 = s2.slice(low2, high2)
    if (seq1.isEmpty || seq2.isEmpty) {
      // shortcut if at least on sequence is empty, the lcs, is empty as well
      Nil
    } else if (seq1 === seq2) {
      // both sequences are equal, the lcs is either of them
      seq1.indices.map(i => (i + low1, i + low2)).toList
    } else if (seq1.startsWith(seq2)) {
      // the second sequence is a prefix of the first one
      // the lcs is the second sequence
      seq2.indices.map(i => (i + low1, i + low2)).toList
    } else if (seq2.startsWith(seq1)) {
      // the first sequence is a prefix of the second one
      // the lcs is the first sequence
      seq1.indices.map(i => (i + low1, i + low2)).toList
    } else {
      // we try to reduce the problem by stripping common suffix and prefix
      val (prefix, middle1, middle2, suffix) = splitPrefixSuffix(seq1, seq2, low1, low2)
      val indexedMiddle1: Vector[T] = middle1.toVector
      val indexedMiddle2: Vector[T] = middle2.toVector
      val offset = prefix.size
      val lengths: Array[Array[Int]] = Array.ofDim[Int](middle1.size + 1, middle2.size + 1)

      // fill up the length matrix
      // impl chosen based on microbenchmarks
      val cols = indexedMiddle1.length
      val rows = indexedMiddle2.length

      @tailrec
      def fillJs(i: Int, j: Int): Unit = {
        if (j < rows) {
          if (indexedMiddle1(i) == indexedMiddle2(j))
            lengths(i + 1)(j + 1) = lengths(i)(j) + 1
          else
            lengths(i + 1)(j + 1) = math.max(lengths(i + 1)(j), lengths(i)(j + 1))
          fillJs(i, j + 1)
        }
      }

      @tailrec
      def fillIs(i: Int): Unit = {
        if (i < cols) {
          fillJs(i, 0)
          fillIs(i + 1)
        }
      }

      fillIs(0)

      // and compute the lcs out of the matrix
      @tailrec
      def loop(idx1: Int, idx2: Int, acc: List[(Int, Int)]): List[(Int, Int)] =
        if (idx1 == 0 || idx2 == 0) {
          acc
        } else if (lengths(idx1)(idx2) == lengths(idx1 - 1)(idx2)) {
          loop(idx1 - 1, idx2, acc)
        } else if (lengths(idx1)(idx2) == lengths(idx1)(idx2 - 1)) {
          loop(idx1, idx2 - 1, acc)
        } else {
          assert(seq1(offset + idx1 - 1) == seq2(offset + idx2 - 1))
          loop(idx1 - 1, idx2 - 1, (low1 + offset + idx1 - 1, low2 + offset + idx2 - 1) :: acc)
        }

      prefix ++ loop(indexedMiddle1.size, indexedMiddle2.size, Nil) ++ suffix
    }
  }

  /* Extract common prefix and suffix from both sequences */
  private def splitPrefixSuffix(seq1: List[T], seq2: List[T], low1: Int, low2: Int): (List[(Int, Int)], List[T], List[T], List[(Int, Int)]) = {
    val size1 = seq1.size
    val size2 = seq2.size
    val prefix =
      seq1.zip(seq2).takeWhile {
        case (e1, e2) => e1 == e2
      }.indices.map(i => (i + low1, i + low2)).toList
    val suffix =
      seq1.reverse.zip(seq2.reverse).takeWhile {
        case (e1, e2) => e1 == e2
      }.indices.map(i => (size1 - i - 1 + low1, size2 - i - 1 + low2)).toList.reverse
    (prefix, seq1.drop(prefix.size).dropRight(suffix.size), seq2.drop(prefix.size).dropRight(suffix.size), suffix)
  }

}
