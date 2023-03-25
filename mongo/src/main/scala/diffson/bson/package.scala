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

import cats.syntax.all._
import com.mongodb.client.model.PushOptions
import com.mongodb.client.model.{Updates => JUpdates}
import org.bson._
import org.bson.conversions.Bson

import scala.jdk.CollectionConverters._

import mongoupdate.Updates

package object bson {

  implicit object BsonJsony extends Jsony[BsonValue] {

    override def eqv(x: BsonValue, y: BsonValue): Boolean =
      (x, y) match {
        case (null, null)          => true
        case (null, _) | (_, null) => false
        case _                     => x.equals(y)
      }

    override def show(t: BsonValue): String = t.toString()

    override def makeObject(fields: Map[String, BsonValue]): BsonValue =
      new BsonDocument(fields.toList.map { case (key, value) => new BsonElement(key, value) }.asJava)

    override def fields(json: BsonValue): Option[Map[String, BsonValue]] =
      json.isDocument().guard[Option].map(_ => json.asDocument().asScala.toMap)

    override def makeArray(values: Vector[BsonValue]): BsonValue =
      new BsonArray(values.asJava)

    override def array(json: BsonValue): Option[Vector[BsonValue]] =
      json.isArray().guard[Option].map(_ => json.asArray().asScala.toVector)

    override def Null: BsonValue = BsonNull.VALUE

  }

  implicit object BsonUpdates extends Updates[List[Bson], BsonValue] {

    override def empty: List[Bson] = Nil

    override def set(base: List[Bson], field: String, value: BsonValue): List[Bson] =
      JUpdates.set(field, value) :: base

    override def unset(base: List[Bson], field: String): List[Bson] =
      JUpdates.unset(field) :: base

    override def pushEach(base: List[Bson], field: String, idx: Int, values: List[BsonValue]): List[Bson] =
      JUpdates.pushEach(field, values.asJava, new PushOptions().position(idx)) :: base

    override def pushEach(base: List[Bson], field: String, values: List[BsonValue]): List[Bson] =
      JUpdates.pushEach(field, values.asJava) :: base

  }

}
