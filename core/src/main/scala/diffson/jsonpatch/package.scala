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

package diffson

import lcs.Lcs

package object jsonpatch {

  object lcsdiff {
    object remembering {
      implicit def JsonDiffDiff[Json: Jsony: Lcs]: Diff[Json, JsonPatch[Json]] =
        new JsonDiff[Json](true, true)(implicitly, implicitly[Lcs[Json]].savedHashes)
    }
    implicit def JsonDiffDiff[Json: Jsony: Lcs]: Diff[Json, JsonPatch[Json]] =
      new JsonDiff[Json](true, false)(implicitly, implicitly[Lcs[Json]].savedHashes)
  }

  object simplediff {
    private implicit def nolcs[Json]: Lcs[Json] = new Lcs[Json] {
      def savedHashes = this
      def lcs(seq1: List[Json], seq2: List[Json], low1: Int, high1: Int, low2: Int, high2: Int): List[(Int, Int)] = Nil
    }
    object remembering {
      implicit def JsonDiffDiff[Json: Jsony]: Diff[Json, JsonPatch[Json]] =
        new JsonDiff[Json](false, true)
    }
    implicit def JsonDiffDiff[Json: Jsony]: Diff[Json, JsonPatch[Json]] =
      new JsonDiff[Json](false, false)
  }

}
