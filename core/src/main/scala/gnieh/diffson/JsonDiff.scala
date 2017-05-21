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

import gnieh.diff._

import scala.annotation.tailrec

trait JsonDiffSupport[JsValue] {
  this: DiffsonInstance[JsValue] =>

  import provider._

  /** Default `JsonDiff` instance that uses the patience algorithm to compute lcs for arrays
   *
   *  @author Lucas Satabin
   */
  object JsonDiff extends JsonDiff(new Patience[JsValue])

  /** Methods to compute diffs between two Json values
   *
   *  @author Lucas Satabin
   */
  class JsonDiff(lcsalg: Lcs[JsValue]) {

    private val diff = new LcsDiff(lcsalg)

    /** Computes the patch from `json1` to `json2`.
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def diff(json1: String, json2: String, remember: Boolean): JsonPatch =
      new JsonPatch(diff(parseJson(json1), parseJson(json2), remember, true, Pointer.root))

    /** Computes the patch from `json1` to `json2` without performing array diff.
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def simpleDiff(json1: String, json2: String, remember: Boolean): JsonPatch =
      new JsonPatch(diff(parseJson(json1), parseJson(json2), remember, false, Pointer.root))

    /** Computes the patch from `json1` to `json2`
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def diff(json1: JsValue, json2: JsValue, remember: Boolean): JsonPatch =
      new JsonPatch(diff(json1, json2, remember, true, Pointer.root))

    /** Computes the patch from `json1` to `json2` without performing array diff.
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def simpleDiff(json1: JsValue, json2: JsValue, remember: Boolean): JsonPatch =
      new JsonPatch(diff(json1, json2, remember, false, Pointer.root))

    /** Computes the patch from `json1` to `json2`
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def diff[T1: Marshaller, T2: Marshaller](json1: T1, json2: T2, remember: Boolean): JsonPatch =
      new JsonPatch(diff(marshall(json1), marshall(json2), remember, true, Pointer.root))

    /** Computes the patch from `json1` to `json2` without performing array diff.
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def simpleDiff[T1: Marshaller, T2: Marshaller](json1: T1, json2: T2, remember: Boolean): JsonPatch =
      new JsonPatch(diff(marshall(json1), marshall(json2), remember, false, Pointer.root))

    private def diff(json1: JsValue, json2: JsValue, remember: Boolean, arrayDiffs: Boolean, pointer: Pointer): List[Operation] = (json1, json2) match {
      case (v1, v2) if v1 == v2                         => Nil // if they are equal, this one is easy...
      case (JsObject(fields1), JsObject(fields2))       => fieldsDiff(fields1.toList, fields2.toList, remember, arrayDiffs, pointer)
      case (JsArray(arr1), JsArray(arr2)) if arrayDiffs => arraysDiff(arr1, arr2, remember, pointer)
      case (_, _)                                       => List(Replace(pointer, json2, if (remember) Some(json1) else None))
    }

    private def fieldsDiff(fields1: List[(String, JsValue)], fields2: List[(String, JsValue)], remember: Boolean, arraysDiff: Boolean, path: Pointer): List[Operation] = {
      // sort fields by name in both objects
      val sorted1 = fields1.sortBy(_._1)
      val sorted2 = fields2.sortBy(_._1)
      @tailrec
      def associate(
        fields1: List[(String, JsValue)],
        fields2: List[(String, JsValue)],
        acc: List[(Option[(String, JsValue)], Option[(String, JsValue)])]): List[(Option[(String, JsValue)], Option[(String, JsValue)])] = (fields1, fields2) match {
        case (f1 :: t1, f2 :: t2) if f1._1 == f2._1 =>
          // same name, associate both
          associate(t1, t2, (Some(f1), Some(f2)) :: acc)
        case (f1 :: t1, f2 :: _) if f1._1 < f2._1 =>
          // the first field is not present in the second object
          associate(t1, fields2, (Some(f1), None) :: acc)
        case (f1 :: _, f2 :: t2) =>
          // the second field is not present in the first object
          associate(fields1, t2, (None, Some(f2)) :: acc)
        case (_, Nil) =>
          fields1.map(Some(_) -> None) ::: acc
        case (Nil, _) =>
          fields2.map(None -> Some(_)) ::: acc
      }
      @tailrec
      def fields(fs: List[(Option[(String, JsValue)], Option[(String, JsValue)])], acc: List[Operation]): List[Operation] = fs match {
        case (Some(f1), Some(f2)) :: tl if f1 == f2 =>
          // allright, nothing changed
          fields(tl, acc)
        case (Some(f1), Some(f2)) :: tl =>
          // same field name, different values
          fields(tl, diff(f1._2, f2._2, remember, arraysDiff, path / f1._1) ::: acc)
        case (Some(f1), None) :: tl =>
          // the field was deleted
          fields(tl, Remove(path / f1._1, if (remember) Some(f1._2) else None) :: acc)
        case (None, Some(f2)) :: tl =>
          // the field was added
          fields(tl, Add(path / f2._1, f2._2) :: acc)
        case _ =>
          acc
      }
      fields(associate(sorted1, sorted2, Nil), Nil)
    }

    private def arraysDiff(arr1: Vector[JsValue], arr2: Vector[JsValue], remember: Boolean, path: Pointer): List[Operation] = {

      // compute the array difference
      val adiff = diff.diff(arr1, arr2)

      // then iterate over them to build the appropriate json-patch operations
      @tailrec
      def loop(didx: Int, idx1: Int, shift1: Int, acc: List[Operation]): List[Operation] =
        if (didx >= adiff.size) {
          acc
        } else adiff(didx) match {
          case Both(start1, end1, start2, end2) =>
            // ok nothing change for this chunk, go to end offset in both arrays
            loop(didx + 1, idx1 + (end1 - start1), shift1, acc)
          case First(start1, end1) =>
            // this chunk is only in the first array, aka it was deleted
            // we add the appropriate delete operations and decrement the shift by
            // the amount of deleted elements
            val length = end1 - start1
            val ops =
              for (idx <- (start1 until end1).toList)
                yield Remove(path / (idx + shift1), if (remember) Some(arr1(idx)) else None)
            loop(didx + 1, idx1 + length, shift1 - length, ops ::: acc)

          case Second(start2, end2) =>
            // this chunk is only in the second array, aka it was added
            // we add the appropriate add operations and increment the shift by
            // the amounbt of deleted elements
            val length = end2 - start2
            val ops =
              for (offset <- (0 until length).toList)
                yield Add(if (idx1 < arr1.size) path / (idx1 + offset + shift1) else path / "-", arr2(start2 + offset))

            loop(didx + 1, idx1, shift1 + length, ops reverse_::: acc)
        }

      val diff1 = loop(0, 0, 0, Nil)

      @tailrec
      def compress(ops: List[Operation], acc: List[Operation]): List[Operation] =
        ops match {
          case Nil      => acc
          case List(op) => op :: acc
          case Add(_ :/ "-", v) :: Remove(prem @ (_ :/ IntIndex(idx)), old) :: rest if idx == arr1.size - 1 =>
            compress(rest, Replace(prem, v, old) :: acc)
          case Add(_ :/ IntIndex(aidx), v) :: Remove(prem @ (_ :/ IntIndex(ridx)), old) :: rest if aidx == ridx =>
            compress(rest, Replace(prem, v, old) :: acc)
          case op :: rest =>
            compress(rest, op :: acc)
        }
      compress(diff1, Nil)

    }

  }

}
