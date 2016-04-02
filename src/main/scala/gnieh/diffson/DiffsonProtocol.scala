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

  implicit def PointerFormat(implicit pointer: JsonPointer): JsonFormat[Pointer] =
    new JsonFormat[Pointer] {

      def write(p: Pointer): JsString =
        JsString(p.toString)

      def read(value: JsValue): Pointer = value match {
        case JsString(s) => pointer.parse(s)
        case _           => throw new FormatException(f"Pointer expected: $value")
      }

    }

  implicit def OperationFormat(implicit pointer: JsonPointer): JsonFormat[Operation] =
    new JsonFormat[Operation] {

      def write(op: Operation): JsObject =
        op match {
          case Add(path, value) =>
            JsObject(
              "op" -> JsString("add"),
              "path" -> JsString(path.toString),
              "value" -> value)
          case Remove(path, None) =>
            JsObject(
              "op" -> JsString("remove"),
              "path" -> JsString(path.toString))
          case Remove(path, Some(old)) =>
            JsObject(
              "op" -> JsString("remove"),
              "path" -> JsString(path.toString),
              "old" -> old)
          case Replace(path, value, None) =>
            JsObject(
              "op" -> JsString("replace"),
              "path" -> JsString(path.toString),
              "value" -> value)
          case Replace(path, value, Some(old)) =>
            JsObject(
              "op" -> JsString("replace"),
              "path" -> JsString(path.toString),
              "value" -> value,
              "old" -> old)
          case Move(from, path) =>
            JsObject(
              "op" -> JsString("move"),
              "from" -> JsString(from.toString),
              "path" -> JsString(path.toString))
          case Copy(from, path) =>
            JsObject(
              "op" -> JsString("copy"),
              "from" -> JsString(from.toString),
              "path" -> JsString(path.toString))
          case Test(path, value) =>
            JsObject(
              "op" -> JsString("test"),
              "path" -> JsString(path.toString),
              "value" -> value)
        }

      def read(value: JsValue): Operation = value match {
        case obj @ JsObject(fields) if fields.contains("op") =>
          fields("op") match {
            case JsString("add") =>
              obj.getFields("path", "value") match {
                case Seq(JsString(path), value) =>
                  Add(pointer.parse(path), value)
                case _ =>
                  throw new FormatException("missing 'path' or 'value' field")
              }
            case JsString("remove") =>
              obj.getFields("path", "old") match {
                case Seq(JsString(path)) =>
                  Remove(pointer.parse(path))
                case Seq(JsString(path), value) =>
                  Remove(pointer.parse(path), Some(value))
                case _ =>
                  throw new FormatException("missing 'path' field")
              }
            case JsString("replace") =>
              obj.getFields("path", "value", "old") match {
                case Seq(JsString(path), value) =>
                  Replace(pointer.parse(path), value)
                case Seq(JsString(path), value, old) =>
                  Replace(pointer.parse(path), value, Some(old))
                case _ =>
                  throw new FormatException("missing 'path' or 'value' field")
              }
            case JsString("move") =>
              obj.getFields("from", "path") match {
                case Seq(JsString(from), JsString(path)) =>
                  Move(pointer.parse(from), pointer.parse(path))
                case _ =>
                  throw new FormatException("missing 'from' or 'path' field")
              }
            case JsString("copy") =>
              obj.getFields("from", "path") match {
                case Seq(JsString(from), JsString(path)) =>
                  Copy(pointer.parse(from), pointer.parse(path))
                case _ =>
                  throw new FormatException("missing 'from' or 'path' field")
              }
            case JsString("test") =>
              obj.getFields("path", "value") match {
                case Seq(JsString(path), value) =>
                  Test(pointer.parse(path), value)
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

  implicit def JsonPatchFormat(implicit pointer: JsonPointer): JsonFormat[JsonPatch] =
    new JsonFormat[JsonPatch] {

      def write(patch: JsonPatch): JsArray =
        JsArray(patch.ops.map(_.toJson).toVector)

      def read(value: JsValue): JsonPatch = value match {
        case JsArray(ops) =>
          JsonPatch(ops.map(_.convertTo[Operation]).toList)
        case _ => throw new FormatException("JsonPatch expected")
      }

    }

}
