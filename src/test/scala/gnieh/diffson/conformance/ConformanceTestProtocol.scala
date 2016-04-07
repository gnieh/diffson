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

import play.api.libs.json._

object ConformanceTestProtocol {

  import DiffsonProtocol._

  implicit val successConformanceTestFormat = Json.format[SuccessConformanceTest]
  implicit val errorConformanceTestFormat = Json.format[ErrorConformanceTest]
  implicit val commentConformanceTestFormat = Json.format[CommentConformanceTest]

  implicit val ConformanceTestFormat = Format[ConformanceTest](Reads {
    case obj @ JsObject(fields) if fields.contains("expected") =>
      obj.validate[SuccessConformanceTest]
    case obj @ JsObject(fields) if fields.contains("error") =>
      obj.validate[ErrorConformanceTest]
    case obj @ JsObject(fields) if fields.contains("patch") =>
      obj.validate[SuccessConformanceTest]
    case obj @ JsObject(fields) if fields.keySet == Set("comment") =>
      obj.validate[CommentConformanceTest]
    case json =>
      throw new Exception(f"Test record expected: $json")
  },
    Writes {
      case success @ SuccessConformanceTest(_, _, _, _, _) =>
        Json.toJson(success)
      case error @ ErrorConformanceTest(_, _, _, _, _) =>
        Json.toJson(error)
      case com @ CommentConformanceTest(_) =>
        Json.toJson(com)
    }
  )

}
