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

package diffson.mongoupdate

/** Typeclass describing the [[https://www.mongodb.com/docs/manual/reference/operator/update/ Mongo Update operators]]
  * necessary to generate a diff.
  */
trait Updates[Update, Bson] {

  def empty: Update

  def set(base: Update, field: String, value: Bson): Update

  def unset(base: Update, field: String): Update

  def pushEach(base: Update, field: String, idx: Int, values: List[Bson]): Update

  def pushEach(base: Update, field: String, values: List[Bson]): Update

}
