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
package bson

import lcs._
import mongoupdate.lcsdiff._

import cats.Show
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._
import de.flapdoodle.embed.mongo.distribution.Version
import mongo4cats.client.MongoClient
import mongo4cats.embedded.EmbeddedMongo
import org.bson._
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import weaver._
import weaver.scalacheck._

import scala.jdk.CollectionConverters._
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates

object ApplyUpdateSpec extends IOSuite with Checkers {

  implicit val lcs: Lcs[BsonValue] = new Patience[BsonValue]

  type Res = MongoClient[IO]

  override def sharedResource: Resource[IO, Res] =
    EmbeddedMongo.start(27017, None, None, Version.V6_0_4) >>
      MongoClient.fromConnectionString[IO]("mongodb://localhost:27017")

  implicit val arbitraryBson: Arbitrary[BsonDocument] = Arbitrary {
    val genLeaf = Gen.oneOf(
      Gen.choose(0, 1000).map(new BsonInt32(_)),
      Gen.alphaNumStr.map(new BsonString(_)),
      Gen.const(new BsonBoolean(true)),
      Gen.const(new BsonBoolean(false)),
      Gen.const(BsonNull.VALUE)
    )

    def genArray(depth: Int, length: Int): Gen[BsonValue] =
      for {
        n <- Gen.choose(length / 3, length / 2)
        c <- Gen.listOfN(n, sizedBson(depth / 2, length / 2))
      } yield new BsonArray(c.asJava)

    def genDoc(depth: Int, length: Int): Gen[BsonDocument] =
      for {
        n <- Gen.choose(length / 3, length / 2)
        c <- Gen.listOfN(n, sizedBson(depth / 2, length / 2))
      } yield new BsonDocument(c.mapWithIndex((v, idx) => new BsonElement(s"elt$idx", v)).asJava)

    def sizedBson(depth: Int, length: Int) =
      if (depth <= 0) genLeaf
      else Gen.frequency((1, genLeaf), (2, genArray(depth, length)), (2, genDoc(depth, length)))

    Gen.sized { depth =>
      Gen.sized { length =>
        genDoc(depth = depth, length = length)
      }
    }
  }

  implicit val showDoc: Show[BsonDocument] = Show.fromToString

  test("apply updates") { client =>
    forall { (bson1: BsonDocument, bson2: BsonDocument) =>
      val id = new BsonObjectId
      bson1.put("_id", id)
      bson2.put("_id", id)

      val diff = (bson1: BsonValue).diff(bson2)
      for {
        db <- client.getDatabase("testdb")
        coll <- db.getCollection("docs")
        doc = mongo4cats.bson.Document.fromJava(new Document(bson1))
        _ <- coll.insertOne(doc)
        update = Updates.combine(diff: _*)
        _ <- coll.updateOne(Filters.eq("_id", id), update)
        foundDoc <- coll.find(Filters.eq("_id", id)).first
      } yield expect.eql(Some(bson2: BsonValue), foundDoc.map(_.toBsonDocument))
      IO.pure(expect(true))
    }
  }

}
