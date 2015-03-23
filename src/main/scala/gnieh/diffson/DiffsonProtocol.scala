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

object DiffsonProtocol extends DefaultJsonProtocol {

  implicit object OperationFormat extends JsonFormat[Operation] {

    def write(op: Operation): JsObject =
      op.toJson

    def read(value: JsValue): Operation = value match {
      case obj @ JsObject(fields) if fields.contains("op") =>
        fields("op") match {
          case JsString("add") =>
            obj.getFields("path", "value") match {
              case Seq(JsString(path), value) =>
                Add(pointer.parse(path), value)
              case _ =>
                deserializationError("Operation expected")
            }
          case JsString("remove") =>
            obj.getFields("path") match {
              case Seq(JsString(path)) =>
                Remove(pointer.parse(path))
              case _ =>
                deserializationError("Operation expected")
            }
          case JsString("replace") =>
            obj.getFields("path", "value") match {
              case Seq(JsString(path), value) =>
                Replace(pointer.parse(path), value)
              case _ =>
                deserializationError("Operation expected")
            }
          case JsString("move") =>
            obj.getFields("from", "path") match {
              case Seq(JsString(from), JsString(path)) =>
                Move(pointer.parse(from), pointer.parse(path))
              case _ =>
                deserializationError("Operation expected")
            }
          case JsString("copy") =>
            obj.getFields("from", "path") match {
              case Seq(JsString(from), JsString(path)) =>
                Copy(pointer.parse(from), pointer.parse(path))
              case _ =>
                deserializationError("Operation expected")
            }
          case JsString("test") =>
            obj.getFields("path", "value") match {
              case Seq(JsString(path), value) =>
                Test(pointer.parse(path), value)
              case _ =>
                deserializationError("Operation expected")
            }
          case op =>
            deserializationError(s"Unknown operation ${op.compactPrint}")
        }
      case _ =>
        deserializationError("Operation expected")
    }
  }

  implicit object JsonPatchFormat extends JsonFormat[JsonPatch] {

    def write(patch: JsonPatch): JsArray =
      JsArray(patch.ops.toJson)

    def read(value: JsValue): JsonPatch = value match {
      case JsArray(ops) =>
        JsonPatch(ops.map(_.convertTo[Operation]).toList)
      case _ => deserializationError("JsonPatch expected")
    }

  }

}
