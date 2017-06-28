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

    /** Computes the patch from `json1` to `json2`.
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def diff(json1: String, json2: String, remember: Boolean): JsonPatch =
      new JsonPatch(diff(parseJson(json1), parseJson(json2), remember, arrayDiffs = true, JsonPointer(Pointer.Root)))

    /** Computes the patch from `json1` to `json2` without performing array diff.
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def simpleDiff(json1: String, json2: String, remember: Boolean): JsonPatch =
      new JsonPatch(diff(parseJson(json1), parseJson(json2), remember, arrayDiffs = false, JsonPointer(Pointer.Root)))

    /** Computes the patch from `json1` to `json2`
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def diff(json1: JsValue, json2: JsValue, remember: Boolean): JsonPatch =
      new JsonPatch(diff(json1, json2, remember, arrayDiffs = true, JsonPointer(Pointer.Root)))

    /** Computes the patch from `json1` to `json2` without performing array diff.
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def simpleDiff(json1: JsValue, json2: JsValue, remember: Boolean): JsonPatch =
      new JsonPatch(diff(json1, json2, remember, arrayDiffs = false, JsonPointer(Pointer.Root)))

    /** Computes the patch from `json1` to `json2`
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def diff[T1: Marshaller, T2: Marshaller](json1: T1, json2: T2, remember: Boolean): JsonPatch =
      new JsonPatch(diff(marshall(json1), marshall(json2), remember, arrayDiffs = true, JsonPointer(Pointer.Root)))

    /** Computes the patch from `json1` to `json2` without performing array diff.
     *  If `remember` is set to true, the removed and replaced value are rememberd in the patch in a field named `old`.
     */
    def simpleDiff[T1: Marshaller, T2: Marshaller](json1: T1, json2: T2, remember: Boolean): JsonPatch =
      new JsonPatch(diff(marshall(json1), marshall(json2), remember, arrayDiffs = false, JsonPointer(Pointer.Root)))

    private def diff(json1: JsValue, json2: JsValue, remember: Boolean, arrayDiffs: Boolean, pointer: JsonPointer): List[Operation] = (json1, json2) match {
      case (v1, v2) if v1 == v2                         => Nil // if they are equal, this one is easy...
      case (JsObject(fields1), JsObject(fields2))       => fieldsDiff(fields1.toList, fields2.toList, remember, arrayDiffs, pointer)
      case (JsArray(arr1), JsArray(arr2)) if arrayDiffs => arraysDiff(arr1.toList, arr2.toList, remember, pointer)
      case (_, _)                                       => List(Replace(pointer, json2, if (remember) Some(json1) else None))
    }

    private def fieldsDiff(fields1: List[(String, JsValue)], fields2: List[(String, JsValue)], remember: Boolean, arraysDiff: Boolean, path: JsonPointer): List[Operation] = {
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

    private def arraysDiff(arr1: List[JsValue], arr2: List[JsValue], remember: Boolean, path: JsonPointer): List[Operation] = {
      // get the longest common subsequence in the array
      val lcs = lcsalg.lcs(arr1, arr2)

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
      def add(arr: List[JsValue], idx: Int, acc: List[Operation]): List[Operation] = arr match {
        case v :: tl => add(tl, idx + 1, Add(path / idx, v) :: acc)
        case Nil     => acc.reverse
      }

      // remove a bunch of array elements starting by the last one in the range
      def remove(from: Int, until: Int, shift: Int, arr: List[JsValue]): List[Operation] =
        (for (idx <- until to from by -1)
          yield Remove(path / idx, if (remember) Some(arr(idx - shift)) else None)).toList

      // now iterate over the first array to computes what was added, what was removed and what was modified
      @tailrec
      def loop(
        arr1: List[JsValue], // the first array
        arr2: List[JsValue], // the second array
        idx1: Int, // current index in the first array
        shift1: Int, // current index shift in the first array (due to elements being add or removed)
        idx2: Int, // current index in the second array
        lcs: List[(Int, Int)], // the list of remaining matching indices
        acc: List[Operation] // the already accumulated result
      ): List[Operation] = (arr1, arr2) match {
        case (_ :: tl1, _) if isCommon1(idx1, lcs) =>
          // all values in arr2 were added until the index of common value
          val until = lcs.head._2
          loop(tl1, arr2.drop(until - idx2 + 1), idx1 + 1, shift1 + until - idx2, until + 1, lcs.tail,
            add(arr2.take(until - idx2), idx1 + shift1, Nil) reverse_::: acc)
        case (_, _ :: tl2) if isCommon2(idx2, lcs) =>
          // all values in arr1 were removed until the index of common value
          val until = lcs.head._1
          loop(arr1.drop(until - idx1 + 1), tl2, until + 1, shift1 - (until - idx1), idx2 + 1, lcs.tail,
            remove(idx1 + shift1, until - 1 + shift1, idx1 + shift1, arr1) reverse_::: acc)
        case (v1 :: tl1, v2 :: tl2) =>
          // values are different, recursively compute the diff of these values
          loop(tl1, tl2, idx1 + 1, shift1, idx2 + 1, lcs, diff(v1, v2, remember, arrayDiffs = true, path / (idx1 + shift1)) reverse_::: acc)
        case (_, Nil) =>
          // all subsequent values in arr1 were removed
          remove(idx1 + shift1, idx1 + arr1.size - 1 + shift1, idx1 + shift1, arr1) reverse_::: acc
        case (Nil, _) =>
          // all subsequent value in arr2 were added
          arr2.map(Add(path / "-", _)) reverse_::: acc
      }

      loop(arr1, arr2, 0, 0, 0, lcs, Nil).reverse
    }

  }

}
