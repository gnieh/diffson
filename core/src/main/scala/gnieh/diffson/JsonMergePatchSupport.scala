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

trait JsonMergePatchSupport[JsValue] {
  this: DiffsonInstance[JsValue] =>

  import provider._

  sealed trait JsonMergePatch {

    def toJson: JsValue

    /** Applies this patch to the given Json valued and returns the patched value */
    def apply(json: JsValue): JsValue

    /** Applies this patch to the given Json valued and returns the patched value */
    def apply(json: String): String =
      compactPrint(apply(parseJson(json)))

    /** Applies this patch to the given Json value, and returns the patched value.
     *  It assumes that the shape of the patched object is the same as the input one.
     *  If it is not the case, an exception will be raised
     */
    def apply[T: Marshaller: Unmarshaller](value: T): T =
      unmarshall[T](apply(marshall(value)))

    override def toString: String = prettyPrint(marshall(this))

  }

  object JsonMergePatch {

    /** Parses a Json patch as per http://tools.ietf.org/html/rfc6902 */
    def parse(patch: String): JsonPatch =
      unmarshall[JsonPatch](parseJson(patch))

    def apply(json: JsValue): JsonPatch =
      unmarshall[JsonPatch](json)

    case class Value(toJson: JsValue) extends JsonMergePatch {
      def apply(json: JsValue): JsValue = toJson
    }

    case class Object(fields: Map[String, JsValue]) extends JsonMergePatch {
      def toJson = JsObject(fields)
      def apply(json: JsValue): JsValue = {
        val toPatch =
          json match {
            case JsObject(toPatch) => toPatch
            case _                 => Map.empty[String, JsValue]
          }
        val patched =
          fields.foldLeft(toPatch) {
            case (acc, (k, JsNull)) =>
              acc - k
            case (acc, (k, p @ Object(_))) =>
              acc.updated(k, p(acc.getOrElse(k, JsNull)))
            case (acc, (k, v)) =>
              acc.updated(k, v)
          }
        JsObject(patched)
      }

    }

  }

  object JsonMergeDiff {

    /** Computes the patch from `json1` to `json2`. */
    def diff(json1: String, json2: String): JsonMergePatch =
      diff(parseJson(json1), parseJson(json2))

    /** Computes the patch from `json1` to `json2`. */
    def diff(json1: JsValue, json2: JsValue): JsonMergePatch =
      (json1, json2) match {
        case (JsObject(fields1), JsObject(fields2)) =>
          val diffed = mapDiff(fields1, fields2)
          JsonMergePatch.Object(diffed)
        case (_, _) =>
          JsonMergePatch.Value(json2)
      }

    /** Computes the patch from `json1` to `json2`. */
    def diff[T1: Marshaller, T2: Marshaller](json1: T1, json2: T2): JsonMergePatch =
      diff(marshall(json1), marshall(json2))

    private def mapDiff(map1: Map[String, JsValue], map2: Map[String, JsValue]): Map[String, JsValue] = {
      val keys1 = map1.keySet
      val keys2 = map2.keySet
      val commonKeys = keys1.intersect(keys2)
      val deletedKeys = keys1.diff(keys2)
      val addedKeys = keys2.diff(keys1)
      val common =
        for {
          k <- commonKeys
          if map1(k) != map2(k)
        } yield (k, diff(map1(k), map2(k)).toJson)
      val deleted =
        for (k <- deletedKeys)
          yield (k, JsNull)
      val added =
        for (k <- addedKeys)
          yield (k, map2(k))
      (common ++ deleted ++ added).toMap
    }

  }

}
