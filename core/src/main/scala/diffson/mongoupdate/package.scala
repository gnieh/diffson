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
