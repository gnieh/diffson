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

import scala.annotation.tailrec

class MongoDiff[Bson, Update](implicit Bson: Jsony[Bson], Update: Updates[Update, Bson]) extends Diff[Bson, Update] {
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
        Eval.now(fields2.foldLeft(acc) { case (acc, (fld, value)) =>
          Update.set(acc, path.append(fld).mkString_("."), value)
        })
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

      // first we commpute the common prefixes and suffixes
      @tailrec
      def commonPrefix(idx: Int): Int =
        if (idx >= length1)
          length1
        else if (arr1(idx) === arr2(idx))
          commonPrefix(idx + 1)
        else
          idx
      val commonPrefixSize = commonPrefix(0)
      @tailrec
      def commonSuffix(idx1: Int, idx2: Int): Int =
        if (idx1 < 0)
          length1
        else if (arr1(idx1) === arr2(idx2))
          commonSuffix(idx1 - 1, idx2 - 1)
        else
          length1 - 1 - idx1
      val commonSuffixSize = commonSuffix(length1 - 1, length2 - 1)

      val update =
        if (commonPrefixSize == length1)
          // all elements are appended
          Update.pushEach(acc, path.mkString_("."), arr2.drop(length1).toList)
        else if (commonSuffixSize == length1)
          // all elements are prepended
          Update.pushEach(acc, path.mkString_("."), 0, arr2.dropRight(length1).toList)
        else if (commonPrefixSize + commonSuffixSize == nbAdded)
          // allements are inserted as a block in the middle
          Update.pushEach(acc,
                          path.mkString_("."),
                          commonPrefixSize,
                          arr2.slice(commonPrefixSize, length2 - commonSuffixSize).toList)
        else
          Update.set(acc, path.mkString_("."), JsArray(arr2))

      Eval.now(update)
    }
  }

}
