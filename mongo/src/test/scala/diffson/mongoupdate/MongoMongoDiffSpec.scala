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

import org.bson.conversions.Bson
import org.bson.{BsonInt32, BsonString, BsonValue}

import mongoupdate._
import test._

object MongoMongoDiffSpec extends MongoDiffSpec[List[Bson], BsonValue] {

  override def int(i: Int): BsonValue = new BsonInt32(i)

  override def string(s: String): BsonValue = new BsonString(s)

}
