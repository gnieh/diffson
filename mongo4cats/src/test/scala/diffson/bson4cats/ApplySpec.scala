package mongo4cats.diffson.bson4cats

import diffson.bson.ApplyUpdateSpec
import diffson.bson4cats._
import mongo4cats.bson.{BsonValue, Document}
import mongo4cats.operations.Update
import org.bson.BsonDocument
import org.bson.conversions.Bson

object ApplySpec extends ApplyUpdateSpec[Update, BsonValue] {

  override def fromBsonDocument(bson: BsonDocument): BsonValue =
    BsonValue.document(Document.fromJava(new org.bson.Document(bson)))

  override def toUpdate(diff: Update): Bson =
    diff.toBson

}
