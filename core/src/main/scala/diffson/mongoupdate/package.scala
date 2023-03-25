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

package diffson

import diffson.lcs.Lcs

package object mongoupdate {

  object lcsdiff {
    implicit def MongoDiffDiff[Bson: Jsony: Lcs, Update](implicit updates: Updates[Update, Bson]): Diff[Bson, Update] =
      new MongoDiff[Bson, Update]()(implicitly, implicitly, implicitly[Lcs[Bson]].savedHashes)
  }

  object simplediff {
    private implicit def nolcs[Bson]: Lcs[Bson] = new Lcs[Bson] {
      def savedHashes = this
      def lcs(seq1: List[Bson], seq2: List[Bson], low1: Int, high1: Int, low2: Int, high2: Int): List[(Int, Int)] = Nil
    }
    implicit def MongoDiffDiff[Bson: Jsony, Update](implicit updates: Updates[Update, Bson]): Diff[Bson, Update] =
      new MongoDiff[Bson, Update]
  }

}
