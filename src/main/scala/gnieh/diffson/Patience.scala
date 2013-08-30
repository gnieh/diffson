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
package gnieh.diffson

import scala.annotation.tailrec

/** Implementation of the patience algorithm [1] to compute the longest common subsequence
 *
 *  [1] http://alfedenzo.livejournal.com/170301.html
 *
 *  @author Lucas Satabin */
class Patience[T] extends Lcs[T] {

  /** An occurrence of a value associated to its index */
  type Occurrence = (T, Int)

  /** Returns occurrences that appear only once in the list, associated with their index */
  private def uniques(l: List[T]): List[Occurrence] = {
    @tailrec
    def loop(l: List[Occurrence], acc: Map[T, Int]): List[Occurrence] = l match {
      case (value, idx) :: tl =>
        if(acc.contains(value))
          // not unique, remove it from the accumulator and go further
          loop(tl, acc - value)
        else
          loop(tl, acc + (value -> idx))
      case Nil =>
        acc.toList
    }
    loop(l.zipWithIndex, Map())
  }

  /** Takes all occurences from the first sequence and order them as in the second sequence if it is present */
  private def common(l1: List[Occurrence], l2: List[Occurrence]): List[(Occurrence, Int)] = {
    @tailrec
    def loop(l: List[Occurrence], acc: List[(Occurrence, Int)]): List[(Occurrence, Int)] = l match {
      case occ :: tl =>
        // find the element in the second sequence if present
        l2.find(_._1 == occ._1) match {
          case Some((_, idx2)) => loop(tl, (occ -> idx2) :: acc)
          case None          => loop(tl, acc)
        }
      case Nil =>
        // sort by order of appearance in the second sequence
        acc sortWith (_._2 < _._2)
    }
    loop(l1, Nil)
  }

  /** Returns the list of elements that appear only once in both l1 and l2 ordered as they appear in l2 with their index in l1 */
  private def uniqueCommons(seq1: Seq[T], seq2: Seq[T]): List[(Occurrence, Int)] = {
    // the values that occur only once in the first sequence
    val uniques1 = uniques(seq1.toList)
    // the values that occur only once in the second sequence
    val uniques2 = uniques(seq2.toList)
    // now order the unique occurrences as they appear in the second list
    common(uniques1, uniques2)
  }

  /** Returns the longest sequence */
  private def longest(l: List[(Occurrence, Int)]): List[(Int, Int)] = {
    if(l.isEmpty) {
      Nil
    } else {
      @tailrec
      def push(idx1: Int, idx2: Int, stacks: List[List[Stacked]], last: Option[Stacked], acc: List[List[Stacked]]): List[List[Stacked]] = stacks match {
        case (stack @ (Stacked(idx, _, _) :: _)) :: tl if idx > idx1 =>
          // we found the right stack
          acc.reverse ::: (Stacked(idx1, idx2, last) :: stack) :: tl
        case (stack @ (stacked :: _)) :: tl =>
          // try the next one
          push(idx1, idx2, tl, Some(stacked), stack :: acc)
        case Nil =>
          // no stack corresponds, create a new one
          acc.reverse ::: List(List(Stacked(idx1, idx2, last)))
        case Nil :: _ =>
          // this case should NEVER happen
          throw new Exception("No empty stack must exist")
      }
      def sort(l: List[(Occurrence, Int)]): List[List[Stacked]] =
        l.foldLeft(List[List[Stacked]]()) { case (acc, ((_, idx1), idx2)) =>
          push(idx1, idx2, acc, None, Nil)
        }
      val sorted = sort(l)
      // this call is safe as we know that the list of occurrence is not empty here and that there are no empty stacks
      val greatest = sorted.last.head
      // make the lcs in increasing order
      greatest.chain.reverse
    }
  }

  /** Computes the longest common subsequence between both sequences.
   *  It is encoded as the list of common indices in the first and the second sequence.
   */
  def lcs(seq1: Seq[T], seq2: Seq[T]): List[(Int, Int)] =
    if(seq1.isEmpty || seq2.isEmpty) {
      // shortcut if at least on sequence is empty, the lcs, is empty as well
      Nil
    } else {
      // the lcs of common elements that appear only once in each sequence
      val longestUniques = longest(uniqueCommons(seq1, seq2))
      // fill the holes with possibly common (not unique) elements
      @tailrec
      def loop(longestUniques: List[(Int, Int)], currIdx1: Int, currIdx2: Int, acc: List[(Int, Int)]): List[(Int, Int)] = longestUniques match {
        case (idx1, idx2) :: tl if idx1 <= currIdx1 || idx2 <= currIdx2 =>
          // we reached one upper bound
          loop(tl, idx1 + 1, idx2 + 1, (idx1, idx2) :: acc)
        case l @ ((idx1, idx2) :: _) if idx1 > currIdx1 && idx2 > currIdx2 =>
          // if this is a non unique common element add it to accumulator
          if(seq1(currIdx1) == seq2(currIdx2)) {
            loop(l, currIdx1 + 1, currIdx2 + 1, (currIdx1, currIdx2) :: acc)
          } else {
            loop(l, currIdx1, currIdx2 + 1, acc)
          }
        case Nil =>
          acc.reverse
        case _ =>
          // due to a bug in pattern matcher, the compiler complains about not exhaustive match
          // though it is
          throw new Exception("This case should NEVER happen")

      }
      // we start with first indices in both sequences
      loop(longestUniques, 0, 0, Nil)
    }

}

private case class Stacked(idx1: Int, idx2: Int, next: Option[Stacked]) {
  lazy val chain: List[(Int, Int)] = next match {
    case Some(stacked) =>
      (idx1, idx2) :: stacked.chain
    case None =>
      List(idx1 -> idx2)
  }
}

