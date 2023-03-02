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
package jsonpatch

import lcs._
import jsonpointer._

import cats.syntax.all._
import cats.data.Chain
import cats.Eval

import scala.annotation.tailrec

class JsonDiff[Json](diffArray: Boolean, rememberOld: Boolean)(implicit J: Jsony[Json], Lcs: Lcs[Json])
    extends Diff[Json, JsonPatch[Json]] {
  def diff(json1: Json, json2: Json): JsonPatch[Json] =
    JsonPatch(diff(json1, json2, Pointer.Root).value.toList)

  private def diff(json1: Json, json2: Json, pointer: Pointer): Eval[Chain[Operation[Json]]] =
    (json1, json2) match {
      case (JsObject(fields1), JsObject(fields2))      => fieldsDiff(fields1.toList, fields2.toList, pointer)
      case (JsArray(arr1), JsArray(arr2)) if diffArray => arraysDiff(arr1.toList, arr2.toList, pointer)
      case _ if json1 === json2                        =>
        // if they are equal, this one is easy...
        Eval.now(Chain.empty)
      case _ => Eval.now(Chain.one(Replace(pointer, json2, if (rememberOld) Some(json1) else None)))
    }

  private def fieldsDiff(fields1: List[(String, Json)],
                         fields2: List[(String, Json)],
                         path: Pointer): Eval[Chain[Operation[Json]]] = {
    // sort fields by name in both objects
    val sorted1 = fields1.sortBy(_._1)
    val sorted2 = fields2.sortBy(_._1)
    @tailrec
    def associate(fields1: List[(String, Json)],
                  fields2: List[(String, Json)],
                  acc: Chain[(Option[(String, Json)], Option[(String, Json)])])
        : Chain[(Option[(String, Json)], Option[(String, Json)])] = (fields1, fields2) match {
      case (f1 :: t1, f2 :: t2) if f1._1 == f2._1 =>
        // same name, associate both
        associate(t1, t2, acc.prepend((Some(f1), Some(f2))))
      case (f1 :: t1, f2 :: _) if f1._1 < f2._1 =>
        // the first field is not present in the second object
        associate(t1, fields2, acc.prepend((Some(f1), None)))
      case (_ :: _, f2 :: t2) =>
        // the second field is not present in the first object
        associate(fields1, t2, acc.prepend((None, Some(f2))))
      case (_, Nil) =>
        Chain.fromSeq(fields1.map(Some(_) -> None)) ++ acc
      case (Nil, _) =>
        Chain.fromSeq(fields2.map(None -> Some(_))) ++ acc
    }

    def fields(fs: List[(Option[(String, Json)], Option[(String, Json)])],
               acc: Chain[Operation[Json]]): Eval[Chain[Operation[Json]]] = fs match {
      case (Some(f1), Some(f2)) :: tl if f1._2 === f2._2 =>
        // all right, nothing changed
        fields(tl, acc)
      case (Some(f1), Some(f2)) :: tl =>
        // same field name, different values
        diff(f1._2, f2._2, path / f1._1).flatMap(d => fields(tl, d ++ acc))
      case (Some(f1), None) :: tl =>
        // the field was deleted
        fields(tl, acc.prepend(Remove[Json](path / f1._1, if (rememberOld) Some(f1._2) else None)))
      case (None, Some(f2)) :: tl =>
        // the field was added
        fields(tl, acc.prepend(Add(path / f2._1, f2._2)))
      case _ =>
        Eval.now(acc)
    }
    fields(associate(sorted1, sorted2, Chain.empty).toList, Chain.empty)
  }

  private def arraysDiff(arr1: List[Json], arr2: List[Json], path: Pointer): Eval[Chain[Operation[Json]]] = {
    // get the longest common subsequence in the array
    val lcs = Lcs.lcs(arr1, arr2)

    // indicates whether the index is in the lcs of the first sequence
    def isCommon1(idx1: Int, lcs: List[(Int, Int)]): Boolean = lcs match {
      case (cidx1, _) :: _ if idx1 == cidx1 => true
      case _                                => false
    }

    // indicates whether the index is in the lcs of the second sequence
    def isCommon2(idx2: Int, lcs: List[(Int, Int)]): Boolean = lcs match {
      case (_, cidx2) :: _ if idx2 == cidx2 => true
      case _                                => false
    }

    // add a bunch of values to an array starting at the specified index
    @tailrec
    def add(arr: List[Json], idx: Int, acc: Chain[Operation[Json]]): Chain[Operation[Json]] = arr match {
      case v :: tl => add(tl, idx + 1, acc.append(Add(path / idx, v)))
      case Nil     => acc
    }

    // remove a bunch of array elements starting by the last one in the range
    def remove(from: Int, until: Int, shift: Int, arr: List[Json]): Chain[Operation[Json]] =
      Chain.fromSeq(
        for (idx <- until to from by -1)
          yield Remove[Json](path / idx, if (rememberOld) Some(arr(idx - shift)) else None))

    // now iterate over the first array to computes what was added, what was removed and what was modified
    def loop(
        arr1: List[Json], // the first array
        arr2: List[Json], // the second array
        idx1: Int, // current index in the first array
        shift1: Int, // current index shift in the first array (due to elements being add or removed)
        idx2: Int, // current index in the second array
        lcs: List[(Int, Int)], // the list of remaining matching indices
        acc: Chain[Operation[Json]] // the already accumulated result
    ): Eval[Chain[Operation[Json]]] = (arr1, arr2) match {
      case (_ :: tl1, _) if isCommon1(idx1, lcs) =>
        // all values in arr2 were added until the index of common value
        val until = lcs.head._2
        loop(tl1,
             arr2.drop(until - idx2 + 1),
             idx1 + 1,
             shift1 + until - idx2,
             until + 1,
             lcs.tail,
             acc ++ add(arr2.take(until - idx2), idx1 + shift1, Chain.empty))
      case (_, _ :: tl2) if isCommon2(idx2, lcs) =>
        // all values in arr1 were removed until the index of common value
        val until = lcs.head._1
        loop(arr1.drop(until - idx1 + 1),
             tl2,
             until + 1,
             shift1 - (until - idx1),
             idx2 + 1,
             lcs.tail,
             acc ++ remove(idx1 + shift1, until - 1 + shift1, idx1 + shift1, arr1))
      case (v1 :: tl1, v2 :: tl2) =>
        // values are different, recursively compute the diff of these values
        diff(v1, v2, path / (idx1 + shift1)).flatMap(d => loop(tl1, tl2, idx1 + 1, shift1, idx2 + 1, lcs, acc ++ d))
      case (_, Nil) =>
        // all subsequent values in arr1 were removed
        Eval.now(acc ++ remove(idx1 + shift1, idx1 + arr1.size - 1 + shift1, idx1 + shift1, arr1))
      case (Nil, _) =>
        // all subsequent value in arr2 were added
        Eval.now(acc ++ Chain.fromSeq(arr2.map(Add(path / "-", _))))
    }

    loop(arr1, arr2, 0, 0, 0, lcs, Chain.empty)
  }
}
