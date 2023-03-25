/*
 * Copyright 2022 Lucas Satabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package diffson
package mongoupdate

import cats.Eval
import cats.data.Chain
import cats.syntax.all._

import lcs.Lcs
import scala.annotation.tailrec

class MongoDiff[Bson, Update](implicit Bson: Jsony[Bson], Update: Updates[Update, Bson], Lcs: Lcs[Bson])
    extends Diff[Bson, Update] {
  private type Path = Chain[String]

  override def diff(bson1: Bson, bson2: Bson): Update =
    diff(bson1, bson2, Chain.empty, Update.empty).value

  private def diff(bson1: Bson, bson2: Bson, path: Path, acc: Update): Eval[Update] =
    (bson1, bson2) match {
      case (JsObject(fields1), JsObject(fields2)) =>
        fieldsDiff(fields1.toList, fields2, path, acc)
      case (JsArray(arr1), JsArray(arr2)) =>
        arrayDiff(arr1, arr2, path, acc)
      case _ if bson1 === bson2 =>
        Eval.now(acc)
      case _ =>
        Eval.now(Update.set(acc, path.mkString_("."), bson2))
    }

  private def fieldsDiff(fields1: List[(String, Bson)],
                         fields2: Map[String, Bson],
                         path: Path,
                         acc: Update): Eval[Update] =
    fields1 match {
      case (fld, value1) :: fields1 =>
        fields2.get(fld) match {
          case Some(value2) =>
            diff(value1, value2, path.append(fld), acc).flatMap(fieldsDiff(fields1, fields2 - fld, path, _))
          case None =>
            fieldsDiff(fields1, fields2, path, Update.unset(acc, path.append(fld).mkString_(".")))
        }
      case Nil =>
        Eval.now(fields2.keys.foldLeft(acc)((acc, fld) => Update.unset(acc, path.append(fld).mkString_("."))))
    }

  private def arrayDiff(arr1: Vector[Bson], arr2: Vector[Bson], path: Path, acc: Update): Eval[Update] = {
    val length1 = arr1.length
    val length2 = arr2.length
    if (length1 == length2) {
      // same number of elements, diff them pairwise
      (acc, 0).tailRecM { case (acc, idx) =>
        if (idx >= length1)
          Eval.now(acc.asRight)
        else
          diff(arr1(idx), arr2(idx), path.append(idx.toString()), acc).map((_, idx + 1).asLeft)
      }
    } else if (length1 > length2) {
      // elements were deleted from the array, this is not supported yet, so replace the entire array
      Eval.now(Update.set(acc, path.mkString_("."), JsArray(arr2)))
    } else {
      val nbAdded = length2 - length1
      // there are some additions, and possibly some modifications
      // elements can be added as a block only
      // the LCS is computed to decide where elements are added
      // if there is several additions in several places
      // or a mix of additions and other modifications,
      // then we just replace the entire array, to avoid conflicts
      val lcs = Lcs.lcs(arr1.toList, arr2.toList)

      @tailrec
      def loop(lcs: List[(Int, Int)], idx1: Int, idx2: Int): Update =
        lcs match {
          case (newIdx1, newIdx2) :: rest =>
            if (newIdx1 == idx1 + 1 && newIdx2 == idx2 + 1) {
              // sequence goes forward in both arrays, continue looping
              loop(rest, newIdx1, newIdx2)
            } else if (idx1 == -1 && newIdx2 == nbAdded) {
              // element are added at the beginning, but we must make sure that the rest
              // of the LCS is the original array itself
              // this is the case if the LCS length is the array length
              if (lcs.length == length1) {
                Update.pushEach(acc, path.mkString_("."), 0, arr2.slice(0, nbAdded).toList)
              } else {
                // otherwise there are some changes that would conflict, replace the entire array
                Update.set(acc, path.mkString_("."), JsArray(arr2))
              }
            } else if (newIdx2 - 1 - idx2 == nbAdded) {
              // there is a bigger gap in original array, it must be where the elements are inserted
              // otherwise we stop and replace the entire array
              // if gap is of the right size, check that the rest of the LCS represents the suffix of both arrays
              if (lcs.length == length1 - newIdx1) {
                Update.pushEach(acc, path.mkString_("."), idx1 + 1, arr2.slice(idx2 + 1, idx2 + 1 + nbAdded).toList)
              } else {
                // otherwise there are some changes that would conflict, replace the entire array
                Update.set(acc, path.mkString_("."), JsArray(arr2))
              }
            } else {
              // otherwise replace the entire array
              Update.set(acc, path.mkString_("."), JsArray(arr2))
            }
          case Nil =>
            // we reached the end of the original array,
            // it means every new element is appended to the end
            Update.pushEach(acc, path.mkString_("."), arr2.slice(idx2 + 1, idx2 + 1 + nbAdded).toList)
        }
      Eval.now(loop(lcs, -1, -1))
    }
  }

}
