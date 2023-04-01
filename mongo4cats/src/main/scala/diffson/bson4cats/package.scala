package diffson

import mongo4cats.bson.BsonValue
import mongo4cats.bson.Document
import diffson.mongoupdate.Updates
import mongo4cats.operations.Update
import mongo4cats.operations
import com.mongodb.client.model.PushOptions

package object bson4cats {

  implicit object BsonJsony extends Jsony[BsonValue] {

    override def eqv(x: BsonValue, y: BsonValue): Boolean =
      x == y

    override def show(t: BsonValue): String = t.toString()

    override def makeObject(fields: Map[String, BsonValue]): BsonValue =
      BsonValue.document(Document(fields))

    override def fields(json: BsonValue): Option[Map[String, BsonValue]] =
      json.asDocument.map(_.toMap)

    override def makeArray(values: Vector[BsonValue]): BsonValue =
      BsonValue.array(values)

    override def array(json: BsonValue): Option[Vector[BsonValue]] =
      json.asList.map(_.toVector)

    override def Null: BsonValue = BsonValue.Null

  }

  implicit object BsonUpdates extends Updates[Update, BsonValue] {

    override def empty: Update = operations.Updates.empty

    override def set(base: Update, field: String, value: BsonValue): Update =
      base.set(field, value)

    override def unset(base: Update, field: String): Update =
      base.unset(field)

    override def pushEach(base: Update, field: String, idx: Int, values: List[BsonValue]): Update =
      base.pushEach(field, values, new PushOptions().position(idx))

    override def pushEach(base: Update, field: String, values: List[BsonValue]): Update =
      base.pushEach(field, values)

  }
}
