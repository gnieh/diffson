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

package diffson.bson

import com.mongodb.client.model.Updates
import diffson.bson.ApplyUpdateSpec
import org.bson.conversions.Bson
import org.bson.{BsonDocument, BsonValue}

object ApplySpec extends ApplyUpdateSpec[List[Bson], BsonValue] {

  override def fromBsonDocument(bson: BsonDocument): BsonValue = bson

  override def toUpdate(diff: List[Bson]): Bson = Updates.combine(diff: _*)

}
