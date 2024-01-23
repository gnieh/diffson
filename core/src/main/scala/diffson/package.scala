/*
 * Copyright 2024 Diffson Project
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

import scala.language.higherKinds

package object diffson {

  object JsArray {
    def apply[Json](values: Vector[Json])(implicit Json: Jsony[Json]): Json =
      Json.makeArray(values)
    def unapply[Json](json: Json)(implicit Json: Jsony[Json]): Option[Vector[Json]] =
      Json.array(json)
  }

  object JsObject {
    def apply[Json](fields: Map[String, Json])(implicit Json: Jsony[Json]): Json =
      Json.makeObject(fields)
    def unapply[Json](json: Json)(implicit Json: Jsony[Json]): Option[Map[String, Json]] =
      Json.fields(json)
  }

  implicit class DiffOps[Json](val json: Json) extends AnyVal {
    def diff[P](that: Json)(implicit Diff: Diff[Json, P]): P =
      Diff.diff(json, that)
  }

  def diff[Json, P](json1: Json, json2: Json)(implicit Diff: Diff[Json, P]): P =
    Diff.diff(json1, json2)

  implicit class PatchOps[P](val patch: P) extends AnyVal {
    def apply[F[_], Json](json: Json)(implicit P: Patch[F, Json, P]): F[Json] =
      P.apply(json, patch)
  }
}
