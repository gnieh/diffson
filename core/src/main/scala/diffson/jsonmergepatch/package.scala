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

import lcs.Lcs

import cats._

import scala.language.higherKinds

package object jsonmergepatch {

  implicit def JsonMergePatchPatch[F[_], Json](implicit F: MonadError[F, Throwable], Json: Jsony[Json]): Patch[F, Json, JsonMergePatch[Json]] =
    new Patch[F, Json, JsonMergePatch[Json]] {
      def apply(json: Json, patch: JsonMergePatch[Json]): F[Json] =
        patch[F](json)
    }

  implicit def JsonMergeDiffDiff[Json: Jsony]: Diff[Json, JsonMergePatch[Json]] =
    new JsonMergeDiff[Json]

}
