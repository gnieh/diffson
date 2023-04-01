package diffson.bson4cats

import diffson.mongoupdate.MongoDiffSpec
import mongo4cats.bson.BsonValue
import mongo4cats.operations.Update

import test._

object DiffSpec extends MongoDiffSpec[Update, BsonValue] {

  override def int(i: Int): BsonValue = BsonValue.int(i)

  override def string(s: String): BsonValue = BsonValue.string(s)

}
