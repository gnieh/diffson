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
package diffson
package jsonmergepatch

import cats._
import cats.implicits._

import scala.language.higherKinds

sealed trait JsonMergePatch[Json] {

  /** Applies this patch to the given Json valued and returns the patched value */
  def apply[F[_]](original: Json)(implicit F: MonadError[F, Throwable]): F[Json]

  def toJson: Json

}

object JsonMergePatch {

  case class Value[Json](toJson: Json) extends JsonMergePatch[Json] {
    def apply[F[_]](original: Json)(implicit F: MonadError[F, Throwable]): F[Json] =
      F.pure(toJson)
  }

  case class Object[Json](fields: Map[String, Json])(implicit Json: Jsony[Json]) extends JsonMergePatch[Json] {
    def toJson = JsObject(fields)
    def apply[F[_]](json: Json)(implicit F: MonadError[F, Throwable]): F[Json] = {
      val toPatch =
        json match {
          case JsObject(toPatch) => toPatch
          case _                 => Map.empty[String, Json]
        }
      val patched =
        fields.toList.foldM(toPatch) {
          case (acc, (k, n)) if n === Json.Null =>
            F.pure(acc - k)
          case (acc, (k, JsObject(flds))) =>
            Object(flds).apply[F](acc.getOrElse(k, Json.Null)).map(acc.updated(k, _))
          case (acc, (k, v)) =>
            F.pure(acc.updated(k, v))
        }
      patched.map(JsObject(_))
    }

  }

}
