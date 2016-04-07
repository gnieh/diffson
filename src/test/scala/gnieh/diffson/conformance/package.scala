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

import play.api.libs.json._

package object conformance {

  import DiffsonProtocol._

  implicit val successConformanceTestFormat = Json.format[SuccessConformanceTest]
  implicit val errorConformanceTestFormat = Json.format[ErrorConformanceTest]
  implicit val commentConformanceTestFormat = Json.format[CommentConformanceTest]

  implicit val ConformanceTestFormat = Reads[ConformanceTest] {
    case obj @ JsObject(fields) if fields.contains("expected") =>
      obj.validate[SuccessConformanceTest]
    case obj @ JsObject(fields) if fields.contains("error") =>
      obj.validate[ErrorConformanceTest]
    case obj @ JsObject(fields) if fields.keySet == Set("comment") =>
      obj.validate[CommentConformanceTest]
    case _ =>
      throw new Exception("Test record expected")
  }

}
