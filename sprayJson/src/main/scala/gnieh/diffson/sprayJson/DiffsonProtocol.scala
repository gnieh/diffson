/*
 * Copyright 2022 Lucas Satabin
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

import jsonpatch._
import jsonpointer._
import jsonmergepatch._

import cats.implicits._

import spray.json._

import scala.util.Try

object DiffsonProtocol extends DiffsonProtocol

trait DiffsonProtocol extends DefaultJsonProtocol {

  implicit val PointerFormat: JsonFormat[Pointer] =
    new JsonFormat[Pointer] {

      def write(p: Pointer): JsString =
        JsString(p.show)

      def read(value: JsValue): Pointer = value match {
        case JsString(s) => Pointer.parse[Try](s).get
        case _           => deserializationError(f"Pointer expected: $value")
      }

    }

  implicit val OperationFormat: RootJsonFormat[Operation[JsValue]] =
    new RootJsonFormat[Operation[JsValue]] {

      def write(op: Operation[JsValue]): JsObject =
        op match {
          case Add(path, value) =>
            JsObject("op" -> JsString("add"), "path" -> JsString(path.show), "value" -> value)
          case Remove(path, Some(old)) =>
            JsObject("op" -> JsString("remove"), "path" -> JsString(path.show), "old" -> old)
          case Remove(path, None) =>
            JsObject("op" -> JsString("remove"), "path" -> JsString(path.show))
          case Replace(path, value, Some(old)) =>
            JsObject("op" -> JsString("replace"), "path" -> JsString(path.show), "value" -> value, "old" -> old)
          case Replace(path, value, None) =>
            JsObject("op" -> JsString("replace"), "path" -> JsString(path.show), "value" -> value)
          case Move(from, path) =>
            JsObject("op" -> JsString("move"), "from" -> JsString(from.show), "path" -> JsString(path.show))
          case Copy(from, path) =>
            JsObject("op" -> JsString("copy"), "from" -> JsString(from.show), "path" -> JsString(path.show))
          case Test(path, value) =>
            JsObject("op" -> JsString("test"), "path" -> JsString(path.show), "value" -> value)
        }

      def read(value: JsValue): Operation[JsValue] = value match {
        case obj @ JsObject(fields) if fields.contains("op") =>
          fields("op") match {
            case JsString("add") =>
              obj.getFields("path", "value") match {
                case Seq(JsString(path), value) =>
                  Add(Pointer.parse[Try](path).get, value)
                case _ =>
                  deserializationError("missing 'path' or 'value' field")
              }
            case JsString("remove") =>
              obj.getFields("path", "old") match {
                case Seq(JsString(path)) =>
                  Remove(Pointer.parse[Try](path).get, None)
                case Seq(JsString(path), old) =>
                  Remove(Pointer.parse[Try](path).get, Some(old))
                case _ =>
                  deserializationError("missing 'path' field")
              }
            case JsString("replace") =>
              obj.getFields("path", "value", "old") match {
                case Seq(JsString(path), value) =>
                  Replace(Pointer.parse[Try](path).get, value, None)
                case Seq(JsString(path), value, old) =>
                  Replace(Pointer.parse[Try](path).get, value, Some(old))
                case _ =>
                  deserializationError("missing 'path' or 'value' field")
              }
            case JsString("move") =>
              obj.getFields("from", "path") match {
                case Seq(JsString(from), JsString(path)) =>
                  Move(Pointer.parse[Try](from).get, Pointer.parse[Try](path).get)
                case _ =>
                  deserializationError("missing 'from' or 'path' field")
              }
            case JsString("copy") =>
              obj.getFields("from", "path") match {
                case Seq(JsString(from), JsString(path)) =>
                  Copy(Pointer.parse[Try](from).get, Pointer.parse[Try](path).get)
                case _ =>
                  deserializationError("missing 'from' or 'path' field")
              }
            case JsString("test") =>
              obj.getFields("path", "value") match {
                case Seq(JsString(path), value) =>
                  Test(Pointer.parse[Try](path).get, value)
                case _ =>
                  deserializationError("missing 'path' or 'value' field")
              }
            case op =>
              deserializationError(f"Unknown operation ${op.compactPrint}")
          }
        case _ =>
          deserializationError(f"Operation expected: $value")
      }
    }

  implicit val JsonPatchFormat: RootJsonFormat[JsonPatch[JsValue]] =
    new RootJsonFormat[JsonPatch[JsValue]] {

      def write(patch: JsonPatch[JsValue]): JsArray =
        JsArray(patch.ops.map(_.toJson).toVector)

      def read(value: JsValue): JsonPatch[JsValue] = value match {
        case JsArray(ops) =>
          new JsonPatch[JsValue](ops.map(_.convertTo[Operation[JsValue]]).toList)
        case _ => deserializationError("JsonPatch[JsValue] expected")
      }

    }

  implicit val JsonMergePatchFormat: JsonFormat[JsonMergePatch[JsValue]] =
    new JsonFormat[JsonMergePatch[JsValue]] {

      def write(patch: JsonMergePatch[JsValue]): JsValue =
        patch match {
          case JsonMergePatch.Object(fields) => JsObject(fields)
          case JsonMergePatch.Value(value)   => value
        }

      def read(json: JsValue): JsonMergePatch[JsValue] =
        json match {
          case JsObject(fields) => JsonMergePatch.Object(fields)
          case _                => JsonMergePatch.Value(json)
        }

    }

}
