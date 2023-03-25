package diffson
package bson

import org.bson.BsonInt32
import org.bson.BsonString
import org.bson.BsonValue
import org.bson.conversions.Bson

import mongoupdate._

object MongoMongoDiffSpec extends MongoDiffSpec[List[Bson], BsonValue] {

  override def int(i: Int): BsonValue = new BsonInt32(i)

  override def string(s: String): BsonValue = new BsonString(s)

}
