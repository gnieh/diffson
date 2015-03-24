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

import spray.json._

package object conformance {

  import DiffsonProtocol._

  implicit val successConformanceTestFormat = jsonFormat5(SuccessConformanceTest)
  implicit val errorConformanceTestFormat = jsonFormat5(ErrorConformanceTest)
  implicit val commentConformanceTestFormat = jsonFormat1(CommentConformanceTest)

  implicit object ConformanceTestFormat extends JsonReader[ConformanceTest] {

    def read(json: JsValue): ConformanceTest = json match {
      case obj @ JsObject(fields) if fields.contains("expected") =>
        obj.convertTo[SuccessConformanceTest]
      case obj @ JsObject(fields) if fields.contains("error") =>
        obj.convertTo[ErrorConformanceTest]
      case obj @ JsObject(fields) if fields.keySet == Set("comment") =>
        obj.convertTo[CommentConformanceTest]
      case _ =>
        deserializationError("Test record expected")
    }

  }

}
