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

object DiffsonProtocol {

  implicit def PointerFormat(implicit pointer: JsonPointer): Format[Pointer] =
    Format[Pointer](
      Reads {
        case JsString(s) => JsSuccess(pointer.parse(s))
        case value       => throw new FormatException(f"Pointer expected: $value")
      },
      Writes(p => JsString(p.toString))
    )

  implicit def OperationFormat(implicit pointer: JsonPointer): Format[Operation] =
    Format[Operation](
      Reads {
        case obj @ JsObject(fields) if fields.contains("op") =>
          fields("op") match {
            case JsString("add") =>
              (fields.get("path"), fields.get("value")) match {
                case (Some(JsString(path)), Some(value)) =>
                  JsSuccess(Add(pointer.parse(path), value))
                case _ =>
                  throw new FormatException("missing 'path' or 'value' field")
              }
            case JsString("remove") =>
              (fields.get("path"), fields.get("old")) match {
                case (Some(JsString(path)), None) =>
                  JsSuccess(Remove(pointer.parse(path)))
                case (Some(JsString(path)), Some(value)) =>
                  JsSuccess(Remove(pointer.parse(path), Some(value)))
                case _ =>
                  throw new FormatException("missing 'path' field")
              }
            case JsString("replace") =>
              (fields.get("path"), fields.get("value"), fields.get("old")) match {
                case (Some(JsString(path)), Some(value), None) =>
                  JsSuccess(Replace(pointer.parse(path), value))
                case (Some(JsString(path)), Some(value), Some(old)) =>
                  JsSuccess(Replace(pointer.parse(path), value, Some(old)))
                case _ =>
                  throw new FormatException("missing 'path' or 'value' field")
              }
            case JsString("move") =>
              (fields.get("from"), fields.get("path")) match {
                case (Some(JsString(from)), Some(JsString(path))) =>
                  JsSuccess(Move(pointer.parse(from), pointer.parse(path)))
                case _ =>
                  throw new FormatException("missing 'from' or 'path' field")
              }
            case JsString("copy") =>
              (fields.get("from"), fields.get("path")) match {
                case (Some(JsString(from)), Some(JsString(path))) =>
                  JsSuccess(Copy(pointer.parse(from), pointer.parse(path)))
                case _ =>
                  throw new FormatException("missing 'from' or 'path' field")
              }
            case JsString("test") =>
              (fields.get("path"), fields.get("value")) match {
                case (Some(JsString(path)), Some(value)) =>
                  JsSuccess(Test(pointer.parse(path), value))
                case _ =>
                  throw new FormatException("missing 'path' or 'value' field")
              }
            case op =>
              throw new FormatException(f"Unknown operation ${Json.stringify(op)}")
          }
        case value =>
          throw new FormatException(f"Operation expected: $value")
      },
      Writes {
        case Add(path, value) =>
          Json.obj(
            "op" -> JsString("add"),
            "path" -> JsString(path.toString),
            "value" -> value)
        case Remove(path, None) =>
          Json.obj(
            "op" -> JsString("remove"),
            "path" -> JsString(path.toString))
        case Remove(path, Some(old)) =>
          Json.obj(
            "op" -> JsString("remove"),
            "path" -> JsString(path.toString),
            "old" -> old)
        case Replace(path, value, None) =>
          Json.obj(
            "op" -> JsString("replace"),
            "path" -> JsString(path.toString),
            "value" -> value)
        case Replace(path, value, Some(old)) =>
          Json.obj(
            "op" -> JsString("replace"),
            "path" -> JsString(path.toString),
            "value" -> value,
            "old" -> old)
        case Move(from, path) =>
          Json.obj(
            "op" -> JsString("move"),
            "from" -> JsString(from.toString),
            "path" -> JsString(path.toString))
        case Copy(from, path) =>
          Json.obj(
            "op" -> JsString("copy"),
            "from" -> JsString(from.toString),
            "path" -> JsString(path.toString))
        case Test(path, value) =>
          Json.obj(
            "op" -> JsString("test"),
            "path" -> JsString(path.toString),
            "value" -> value)
      }
    )

  implicit def JsonPatchFormat(implicit pointer: JsonPointer): Format[JsonPatch] =
    Format[JsonPatch](
      Reads[JsonPatch] {
        case JsArray(ops) =>
          JsSuccess(JsonPatch(ops.map(_.as[Operation]).toList))
        case _ => throw new FormatException("JsonPatch expected")
      },
      Writes(patch => JsArray(patch.ops.map(Json.toJson(_)).toVector))
    )

}
