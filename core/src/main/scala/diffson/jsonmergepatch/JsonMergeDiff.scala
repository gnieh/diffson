/*
 * Copyright 2022 Typelevel
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
package jsonmergepatch

class JsonMergeDiff[Json](implicit Json: Jsony[Json]) extends Diff[Json, JsonMergePatch[Json]] {

  /** Computes the patch from `json1` to `json2`. */
  def diff(json1: Json, json2: Json): JsonMergePatch[Json] =
    (json1, json2) match {
      case (JsObject(fields1), JsObject(fields2)) =>
        val diffed = mapDiff(fields1, fields2)
        JsonMergePatch.Object(diffed)
      case (_, _) =>
        JsonMergePatch.Value(json2)
    }

  private def mapDiff(map1: Map[String, Json], map2: Map[String, Json]): Map[String, Json] = {
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
        yield (k, Json.Null)
    val added =
      for (k <- addedKeys)
        yield (k, map2(k))
    (common ++ deleted ++ added).toMap
  }

}
