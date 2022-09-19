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

import cats._

/** Type-class that describes what is needed for an object
 *  to be able to be diffed as a json like object.
 */
trait Jsony[Json] extends Eq[Json] with Show[Json] {
  def makeObject(fields: Map[String, Json]): Json
  def fields(json: Json): Option[Map[String, Json]]
  def makeArray(values: Vector[Json]): Json
  def array(json: Json): Option[Vector[Json]]
  def Null: Json
}
