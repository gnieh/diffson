/*
 * Copyright 2024 Diffson Project
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
package sprayJson

import jsonpatch.conformance._

import spray.json._

import scala.io.Source

class SprayJsonConformance extends TestRfcConformance[JsValue] with SprayJsonTestProtocol {
  import DiffsonProtocol._

  implicit lazy val successConformanceTestUnmarshaller: RootJsonFormat[SuccessConformanceTest] = jsonFormat5(
    SuccessConformanceTest(_, _, _, _, _))
  implicit lazy val errorConformanceTestUnmarshaller: RootJsonFormat[ErrorConformanceTest] = jsonFormat5(
    ErrorConformanceTest(_, _, _, _, _))
  implicit lazy val commentConformanceTestUnMarshaller: RootJsonFormat[CommentConformanceTest] = jsonFormat1(
    CommentConformanceTest(_))

  implicit object conformanceTestUnmarshaller extends JsonFormat[ConformanceTest] {

    def read(json: JsValue): ConformanceTest = json match {
      case obj @ JsObject(fields) if fields.contains("error") =>
        obj.convertTo[ErrorConformanceTest]
      case obj @ JsObject(fields) if fields.contains("doc") =>
        obj.convertTo[SuccessConformanceTest]
      case obj @ JsObject(fields) if fields.keySet == Set("comment") =>
        obj.convertTo[CommentConformanceTest]
      case _ =>
        deserializationError(f"Test record expected but got $json")
    }

    def write(test: ConformanceTest): JsValue = test match {
      case success @ SuccessConformanceTest(_, _, _, _, _) => success.toJson
      case error @ ErrorConformanceTest(_, _, _, _, _)     => error.toJson
      case comment @ CommentConformanceTest(_)             => comment.toJson
    }

  }

  def load(path: String): List[ConformanceTest] =
    JsonParser(Source.fromURL(getClass.getResource(path)).mkString).convertTo[List[ConformanceTest]]

}
