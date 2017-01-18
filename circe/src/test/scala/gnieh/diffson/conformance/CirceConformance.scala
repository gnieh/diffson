/*
* This file is part of the diffson project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package gnieh.diffson
package conformance

import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import cats.implicits._

import scala.io.Source

class CirceConformance extends TestRfcConformance[Json, CirceInstance](circe) {

  import circe._

  // [[io.circe.Json.JArray]] is private
  type JsArray = io.circe.Json

  implicit lazy val successConformanceTestUnmarshaller: Decoder[SuccessConformanceTest] =
    deriveDecoder[SuccessConformanceTest]

  implicit lazy val errorConformanceTestUnmarshaller: Decoder[ErrorConformanceTest] =
    deriveDecoder[ErrorConformanceTest]

  implicit lazy val commentConformanceTestUnMarshaller: Decoder[CommentConformanceTest] =
    deriveDecoder[CommentConformanceTest]

  implicit lazy val conformanceTestUnmarshaller: Decoder[ConformanceTest] = Decoder.instance[ConformanceTest] { c: HCursor =>
    val fields = c.fieldSet.getOrElse(Set())
    if (fields contains "error")
      c.as[ErrorConformanceTest]
    else if (fields contains "doc")
      c.as[SuccessConformanceTest]
    else if (fields == Set("comment"))
      c.as[CommentConformanceTest]
    else
      Left[DecodingFailure, ConformanceTest](DecodingFailure(f"Test record expected but got ${c.top.get.spaces2}", Nil))
  }

  def load(path: String): List[ConformanceTest] =
    io.circe.jawn.decode[List[ConformanceTest]](Source.fromURL(getClass.getResource(path)).mkString).valueOr(throw _)

}
