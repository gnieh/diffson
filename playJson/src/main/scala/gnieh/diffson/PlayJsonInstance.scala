/*
* This file is part of the diffson project.
* Copyright (c) 2016 Lucas Satabin
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

object playJson extends PlayJsonInstance

class PlayJsonInstance extends DiffsonInstance[JsValue] {

  object DiffsonProtocol {

    implicit val PointerFormat: Format[Pointer] =
      Format[Pointer](
        Reads {
          case JsString(s) => JsSuccess(Pointer.parse(s))
          case value       => throw new FormatException(f"Pointer expected: $value")
        },
        Writes(p => JsString(p.serialize)))

    implicit val OperationFormat: Format[Operation] =
      Format[Operation](
        Reads {
          case obj @ JsObject(fields) if fields.contains("op") =>
            fields("op") match {
              case JsString("add") =>
                (fields.get("path"), fields.get("value")) match {
                  case (Some(JsString(path)), Some(value)) =>
                    JsSuccess(Add(Pointer.parse(path), value))
                  case _ =>
                    throw new FormatException("missing 'path' or 'value' field")
                }
              case JsString("remove") =>
                (fields.get("path"), fields.get("old")) match {
                  case (Some(JsString(path)), None) =>
                    JsSuccess(Remove(Pointer.parse(path)))
                  case (Some(JsString(path)), Some(value)) =>
                    JsSuccess(Remove(Pointer.parse(path), Some(value)))
                  case _ =>
                    throw new FormatException("missing 'path' field")
                }
              case JsString("replace") =>
                (fields.get("path"), fields.get("value"), fields.get("old")) match {
                  case (Some(JsString(path)), Some(value), None) =>
                    JsSuccess(Replace(Pointer.parse(path), value))
                  case (Some(JsString(path)), Some(value), Some(old)) =>
                    JsSuccess(Replace(Pointer.parse(path), value, Some(old)))
                  case _ =>
                    throw new FormatException("missing 'path' or 'value' field")
                }
              case JsString("move") =>
                (fields.get("from"), fields.get("path")) match {
                  case (Some(JsString(from)), Some(JsString(path))) =>
                    JsSuccess(Move(Pointer.parse(from), Pointer.parse(path)))
                  case _ =>
                    throw new FormatException("missing 'from' or 'path' field")
                }
              case JsString("copy") =>
                (fields.get("from"), fields.get("path")) match {
                  case (Some(JsString(from)), Some(JsString(path))) =>
                    JsSuccess(Copy(Pointer.parse(from), Pointer.parse(path)))
                  case _ =>
                    throw new FormatException("missing 'from' or 'path' field")
                }
              case JsString("test") =>
                (fields.get("path"), fields.get("value")) match {
                  case (Some(JsString(path)), Some(value)) =>
                    JsSuccess(Test(Pointer.parse(path), value))
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
              "path" -> JsString(path.serialize),
              "value" -> value)
          case Remove(path, None) =>
            Json.obj(
              "op" -> JsString("remove"),
              "path" -> JsString(path.serialize))
          case Remove(path, Some(old)) =>
            Json.obj(
              "op" -> JsString("remove"),
              "path" -> JsString(path.serialize),
              "old" -> old)
          case Replace(path, value, None) =>
            Json.obj(
              "op" -> JsString("replace"),
              "path" -> JsString(path.serialize),
              "value" -> value)
          case Replace(path, value, Some(old)) =>
            Json.obj(
              "op" -> JsString("replace"),
              "path" -> JsString(path.serialize),
              "value" -> value,
              "old" -> old)
          case Move(from, path) =>
            Json.obj(
              "op" -> JsString("move"),
              "from" -> JsString(from.serialize),
              "path" -> JsString(path.serialize))
          case Copy(from, path) =>
            Json.obj(
              "op" -> JsString("copy"),
              "from" -> JsString(from.serialize),
              "path" -> JsString(path.serialize))
          case Test(path, value) =>
            Json.obj(
              "op" -> JsString("test"),
              "path" -> JsString(path.serialize),
              "value" -> value)
        })

    implicit val JsonPatchFormat: Format[JsonPatch] =
      Format[JsonPatch](
        Reads[JsonPatch] {
          case JsArray(ops) =>
            JsSuccess(JsonPatch(ops.map(_.as[Operation]).toList))
          case _ => throw new FormatException("JsonPatch expected")
        },
        Writes(patch => JsArray(patch.ops.map(Json.toJson(_)).toVector)))

  }

  object provider extends JsonProvider {

    type Marshaller[T] = Writes[T]
    type Unmarshaller[T] = Reads[T]

    val JsNull: JsValue =
      play.api.libs.json.JsNull

    def applyArray(elems: Vector[JsValue]): JsValue =
      play.api.libs.json.JsArray(elems)

    def applyObject(fields: Map[String, JsValue]): JsValue =
      play.api.libs.json.JsObject(fields)

    def compactPrint(value: JsValue): String =
      Json.stringify(value)

    def marshall[T: Marshaller](value: T): JsValue =
      Json.toJson(value)

    def unmarshall[T: Unmarshaller](value: JsValue): T =
      value.as[T]

    def parseJson(s: String): JsValue =
      Json.parse(s)

    implicit val patchMarshaller: Marshaller[JsonPatch] =
      DiffsonProtocol.JsonPatchFormat

    implicit val patchUnmarshaller: Unmarshaller[JsonPatch] =
      DiffsonProtocol.JsonPatchFormat

    def prettyPrint(value: JsValue): String =
      Json.prettyPrint(value)

    def unapplyArray(value: JsValue): Option[Vector[JsValue]] = value match {
      case play.api.libs.json.JsArray(elems) =>
        Some(elems.toVector)
      case _ =>
        None
    }

    def unapplyObject(value: JsValue): Option[Map[String, JsValue]] = value match {
      case play.api.libs.json.JsObject(fields) =>
        Some(fields.toMap)
      case _ =>
        None
    }

  }

}
