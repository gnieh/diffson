/*
 * Copyright 2022 Typelevel
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
package playJson

import jsonpatch.conformance._

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.io.Source

class PlayJsonConformance extends TestRfcConformance[JsValue] with PlayJsonTestProtocol {

  type JsArray = play.api.libs.json.JsArray

  implicit lazy val successConformanceTestUnmarshaller: Reads[SuccessConformanceTest] =
    ((JsPath \ "doc").read[JsValue] and
      (JsPath \ "patch").read[JsArray] and
      (JsPath \ "expected").readNullable[JsValue] and
      (JsPath \ "comment").readNullable[String] and
      (JsPath \ "disabled").readNullable[Boolean])(SuccessConformanceTest.apply _)

  implicit lazy val errorConformanceTestUnmarshaller: Reads[ErrorConformanceTest] = ((JsPath \ "doc").read[JsValue] and
    (JsPath \ "patch").read[JsArray] and
    (JsPath \ "error").read[String] and
    (JsPath \ "comment").readNullable[String] and
    (JsPath \ "disabled").readNullable[Boolean])(ErrorConformanceTest.apply _)

  implicit lazy val commentConformanceTestUnMarshaller: Reads[CommentConformanceTest] =
    (JsPath \ "comment").read[String].map(CommentConformanceTest(_))

  implicit lazy val conformanceTestUnmarshaller: Reads[ConformanceTest] = Reads {
    case obj @ JsObject(fields) if fields.contains("error") =>
      JsSuccess(obj.as[ErrorConformanceTest])
    case obj @ JsObject(fields) if fields.contains("doc") =>
      JsSuccess(obj.as[SuccessConformanceTest])
    case obj @ JsObject(fields) if fields.keySet == Set("comment") =>
      JsSuccess(obj.as[CommentConformanceTest])
    case json =>
      throw new Exception(f"Test record expected but got ${Json.stringify(json)}")
  }

  def load(path: String): List[ConformanceTest] =
    Json.parse(Source.fromURL(getClass.getResource(path)).mkString).as[List[ConformanceTest]]

}
