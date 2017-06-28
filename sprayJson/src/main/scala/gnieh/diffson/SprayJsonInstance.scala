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

import spray.json._

object sprayJson extends SprayJsonInstance

class SprayJsonInstance extends DiffsonInstance[JsValue] {

  object DiffsonProtocol extends DiffsonProtocol

  trait DiffsonProtocol extends DefaultJsonProtocol {

    implicit val PointerFormat: JsonFormat[JsonPointer] =
      new JsonFormat[JsonPointer] {

        def write(p: JsonPointer): JsString =
          JsString(p.serialize)

        def read(value: JsValue): JsonPointer = value match {
          case JsString(s) => JsonPointer.parse(s)
          case _           => throw new FormatException(f"Pointer expected: $value")
        }

      }

    implicit val OperationFormat: RootJsonFormat[Operation] =
      new RootJsonFormat[Operation] {

        def write(op: Operation): JsObject =
          op match {
            case Add(path, value) =>
              JsObject(
                "op" -> JsString("add"),
                "path" -> JsString(path.serialize),
                "value" -> value)
            case Remove(path, None) =>
              JsObject(
                "op" -> JsString("remove"),
                "path" -> JsString(path.serialize))
            case Remove(path, Some(old)) =>
              JsObject(
                "op" -> JsString("remove"),
                "path" -> JsString(path.serialize),
                "old" -> old)
            case Replace(path, value, None) =>
              JsObject(
                "op" -> JsString("replace"),
                "path" -> JsString(path.serialize),
                "value" -> value)
            case Replace(path, value, Some(old)) =>
              JsObject(
                "op" -> JsString("replace"),
                "path" -> JsString(path.serialize),
                "value" -> value,
                "old" -> old)
            case Move(from, path) =>
              JsObject(
                "op" -> JsString("move"),
                "from" -> JsString(from.serialize),
                "path" -> JsString(path.serialize))
            case Copy(from, path) =>
              JsObject(
                "op" -> JsString("copy"),
                "from" -> JsString(from.serialize),
                "path" -> JsString(path.serialize))
            case Test(path, value) =>
              JsObject(
                "op" -> JsString("test"),
                "path" -> JsString(path.serialize),
                "value" -> value)
          }

        def read(value: JsValue): Operation = value match {
          case obj @ JsObject(fields) if fields.contains("op") =>
            fields("op") match {
              case JsString("add") =>
                obj.getFields("path", "value") match {
                  case Seq(JsString(path), value) =>
                    Add(JsonPointer.parse(path), value)
                  case _ =>
                    throw new FormatException("missing 'path' or 'value' field")
                }
              case JsString("remove") =>
                obj.getFields("path", "old") match {
                  case Seq(JsString(path)) =>
                    Remove(JsonPointer.parse(path))
                  case Seq(JsString(path), value) =>
                    Remove(JsonPointer.parse(path), Some(value))
                  case _ =>
                    throw new FormatException("missing 'path' field")
                }
              case JsString("replace") =>
                obj.getFields("path", "value", "old") match {
                  case Seq(JsString(path), value) =>
                    Replace(JsonPointer.parse(path), value)
                  case Seq(JsString(path), value, old) =>
                    Replace(JsonPointer.parse(path), value, Some(old))
                  case _ =>
                    throw new FormatException("missing 'path' or 'value' field")
                }
              case JsString("move") =>
                obj.getFields("from", "path") match {
                  case Seq(JsString(from), JsString(path)) =>
                    Move(JsonPointer.parse(from), JsonPointer.parse(path))
                  case _ =>
                    throw new FormatException("missing 'from' or 'path' field")
                }
              case JsString("copy") =>
                obj.getFields("from", "path") match {
                  case Seq(JsString(from), JsString(path)) =>
                    Copy(JsonPointer.parse(from), JsonPointer.parse(path))
                  case _ =>
                    throw new FormatException("missing 'from' or 'path' field")
                }
              case JsString("test") =>
                obj.getFields("path", "value") match {
                  case Seq(JsString(path), value) =>
                    Test(JsonPointer.parse(path), value)
                  case _ =>
                    throw new FormatException("missing 'path' or 'value' field")
                }
              case op =>
                throw new FormatException(f"Unknown operation ${op.compactPrint}")
            }
          case _ =>
            throw new FormatException(f"Operation expected: $value")
        }
      }

    implicit val JsonPatchFormat: RootJsonFormat[JsonPatch] =
      new RootJsonFormat[JsonPatch] {

        def write(patch: JsonPatch): JsArray =
          JsArray(patch.ops.map(_.toJson).toVector)

        def read(value: JsValue): JsonPatch = value match {
          case JsArray(ops) =>
            new JsonPatch(ops.map(_.convertTo[Operation]).toList)
          case _ => throw new FormatException("JsonPatch expected")
        }

      }

  }

  object provider extends JsonProvider {

    type Marshaller[T] = JsonWriter[T]
    type Unmarshaller[T] = JsonReader[T]

    val JsNull: JsValue =
      spray.json.JsNull

    def applyArray(elems: Vector[JsValue]): JsValue =
      spray.json.JsArray(elems)

    def applyObject(fields: Map[String, JsValue]): JsValue =
      spray.json.JsObject(fields)

    def compactPrint(value: JsValue): String =
      value.compactPrint

    def marshall[T: Marshaller](value: T): JsValue =
      value.toJson

    def unmarshall[T: Unmarshaller](value: JsValue): T =
      value.convertTo[T]

    def parseJson(s: String): JsValue =
      JsonParser(s)

    implicit val patchMarshaller: Marshaller[JsonPatch] =
      DiffsonProtocol.JsonPatchFormat

    implicit val patchUnmarshaller: Unmarshaller[JsonPatch] =
      DiffsonProtocol.JsonPatchFormat

    def prettyPrint(value: JsValue): String =
      value.prettyPrint

    def unapplyArray(value: JsValue): Option[Vector[JsValue]] = value match {
      case spray.json.JsArray(elems) =>
        Some(elems)
      case _ =>
        None
    }

    def unapplyObject(value: JsValue): Option[Map[String, JsValue]] = value match {
      case spray.json.JsObject(fields) =>
        Some(fields)
      case _ =>
        None
    }

  }

}
