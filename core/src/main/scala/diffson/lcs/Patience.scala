/*
* This file is part of the diffson project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package diffson.lcs

import cats.Eq
import cats.implicits._

import scala.annotation.tailrec
import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

/** Implementation of the patience algorithm [1] to compute the longest common subsequence
 *
 *  [1] http://alfedenzo.livejournal.com/170301.html
 *
 *  @param withFallback whether to fallback to classic LCS when patience could not find the LCS
 *  @author Lucas Satabin
 */
class Patience[T: Eq](withFallback: Boolean = true) extends Lcs[T] {

  // algorithm we fall back to when patience algorithm is unable to find the LCS
  private val classicLcs =
    if (withFallback) Some(new DynamicProgLcs[T]) else None

  /** An occurrence of a value associated to its index */
  type Occurrence = (T, Int)

  /** Returns occurrences that appear only once in the list, associated with their index */
  private def uniques(l: List[T]): Map[T, Int] = {
    @tailrec
    def loop(l: List[Occurrence], acc: Map[T, Int]): Map[T, Int] = l match {
      case (value, idx) :: tl =>
        if (acc.contains(value))
          // not unique, remove it from the accumulator and go further
          loop(tl, acc - value)
        else
          loop(tl, acc + (value -> idx))
      case Nil =>
        acc
    }
    loop(l.zipWithIndex, Map.empty)
  }

  /** Takes all occurences from the first sequence and order them as in the second sequence if it is present */
  private def common(l1: Map[T, Int], l2: Map[T, Int]): List[(Occurrence, Int)] = {
    @tailrec
    def loop(l: List[Occurrence], acc: List[(Occurrence, Int)]): List[(Occurrence, Int)] = l match {
      case occ :: tl =>
        // find the element in the second sequence if present
        l2.get(occ._1) match {
          case Some(idx2) => loop(tl, (occ -> idx2) :: acc)
          case None       => loop(tl, acc)
        }
      case Nil =>
        // sort by order of appearance in the second sequence
        acc sortWith (_._2 < _._2)
    }
    loop(l1.toList, Nil)
  }

  /** Returns the list of elements that appear only once in both l1 and l2 ordered as they appear in l2 with their index in l1 */
  private def uniqueCommons(seq1: List[T], seq2: List[T]): List[(Occurrence, Int)] = {
    // the values that occur only once in the first sequence
    val uniques1 = uniques(seq1)
    // the values that occur only once in the second sequence
    val uniques2 = uniques(seq2)
    // now order the unique occurrences as they appear in the second list
    common(uniques1, uniques2)
  }

  /** Returns the longest sequence */
  private def longest(l: List[(Occurrence, Int)]): List[(Int, Int)] = {
    if (l.isEmpty) {
      Nil
    } else {
      type Stack = List[Stacked]

      def push(idx1: Int, idx2: Int, stacks: TreeMap[Int, Stack]): TreeMap[Int, Stack] = {
        stacks.iteratorFrom(idx1).take(1).toList.headOption match {
          case None =>
            // corresponding stack not found, create a new one
            val chainCont = stacks.lastOption.flatMap(_._2.headOption)
            stacks.updated(idx1, Stacked(idx1, idx2, chainCont) :: Nil)
          case Some((idx, oldStack)) =>
            // we found the right stack, replace it by new version
            val chainCont = {
              // we have to find a previous stack
              // don't know how efficient `until` is...
              stacks.until(idx).lastOption.flatMap(_._2.headOption)
            }
            (stacks - idx).updated(idx1, Stacked(idx1, idx2, chainCont) :: oldStack)
        }
      }

      def sort(l: List[(Occurrence, Int)]): TreeMap[Int, Stack] = {
        // foreach item push it onto earliest stack for which: stack.idx1 > item.idx1
        // or create a new stack for it if none can be found

        // stacks are kept in a treeMap (minValue -> stack)
        // it makes it efficient to find the correct stack to update

        l.foldLeft(TreeMap.empty[Int, Stack]) {
          case (acc, ((_, idx1), idx2)) =>
            push(idx1, idx2, acc)
        }
      }
      val sorted = sort(l)
      // this call is safe as we know that the list of occurrence is not empty here and that there are no empty stacks
      val greatest = sorted.last._2.head
      // make the lcs in increasing order
      greatest.chain
    }
  }

  /** Checks if two sequences have at least one common element */
  private def haveCommonElements(s1: List[T], s2: List[T]): Boolean = {
    val s2Set = s2.toSet
    s1.exists(s2Set)
  }

  /** Computes the longest common subsequence between both sequences.
   *  It is encoded as the list of common indices in the first and the second sequence.
   */
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
    } else if (!haveCommonElements(seq1, seq2)) {
      // sequences have no common elements
      Nil
    } else {
      // fill the holes with possibly common (not unique) elements
      def loop(low1: Int, low2: Int, high1: Int, high2: Int, acc: List[(Int, Int)]): List[(Int, Int)] =
        if (low1 == high1 || low2 == high2) {
          acc
        } else {
          var lastPos1 = low1 - 1
          var lastPos2 = low2 - 1
          var answer = acc
          for ((p1, p2) <- longest(uniqueCommons(seq1.view(low1, high1).toList, seq2.view(low2, high2).toList))) {
            // recurse between lines which are unique in each sequence
            val pos1 = p1 + low1
            val pos2 = p2 + low2
            // most of the time we have sequences of similar entries
            if (lastPos1 + 1 != pos1 || lastPos2 + 1 != pos2)
              answer = loop(lastPos1 + 1, lastPos2 + 1, pos1, pos2, answer)
            lastPos1 = pos1
            lastPos2 = pos2
            answer = (pos1, pos2) :: answer
          }
          if (answer.size > acc.size) {
            // the size of the accumulator increased, find
            // matches between the last match and the end
            loop(lastPos1 + 1, lastPos2 + 1, high1, high2, answer)
          } else if (seq1(low1) == seq2(low2)) {
            // find lines that match at the beginning
            var newLow1 = low1
            var newLow2 = low2
            while (newLow1 < high1 && newLow2 < high2 && seq1(newLow1) == seq2(newLow2)) {
              answer = (newLow1, newLow2) :: answer
              newLow1 += 1
              newLow2 += 1
            }
            loop(newLow1, newLow2, high1, high2, answer)
          } else if (seq1(high1 - 1) == seq2(high2 - 1)) {
            // find lines that match at the end
            var newHigh1 = high1 - 1
            var newHigh2 = high2 - 1
            while (newHigh1 > low1 && newHigh2 > low2 && seq1(newHigh1 - 1) == seq2(newHigh2 - 1)) {
              newHigh1 -= 1
              newHigh2 -= 1
            }
            answer = loop(lastPos1 + 1, lastPos2 + 1, newHigh1, newHigh2, answer)
            for (i <- 0 until (high1 - newHigh1))
              answer = (newHigh1 + i, newHigh2 + i) :: answer
            answer
          } else {
            classicLcs match {
              case Some(classicLcs) =>
                // fall back to classic LCS algorithm when there is no unique common elements
                // between both sequences and they have no common prefix nor suffix
                // raw patience algorithm is not good for finding LCS in such cases
                classicLcs.lcs(seq1, seq2, low1, high1, low2, high2) reverse_::: answer

              case _ =>
                answer
            }
          }

        }
      // we start with first indices in both sequences
      loop(low1, low2, high1, high2, Nil).reverse
    }
  }

}

private case class Stacked(idx1: Int, idx2: Int, next: Option[Stacked]) {
  def chain: List[(Int, Int)] = {
    @tailrec
    def loop(stacked: Stacked, acc: List[(Int, Int)]): List[(Int, Int)] =
      stacked.next match {
        case Some(next) =>
          loop(next, (stacked.idx1, stacked.idx2) :: acc)
        case None =>
          (stacked.idx1, stacked.idx2) :: acc
      }
    loop(this, Nil)
  }
}

