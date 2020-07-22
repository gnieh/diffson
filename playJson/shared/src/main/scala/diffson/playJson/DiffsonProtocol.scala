/*
 * This file is part of the diffson project.
 * Copyright (c) 2019 Lucas Satabin
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
package diffson
package playJson

import jsonpatch._
import jsonpointer._
import jsonmergepatch._

import cats._
import cats.implicits._

import play.api.libs.json._

import scala.annotation.tailrec

object DiffsonProtocol {

  private def errorToException(error: JsError): Exception =
    JsError.toFlatForm(error) match {
      case Seq((_, Seq(e, _*)), _*) => new PatchException(e.message)
      case _                        => new PatchException("Empty json error")
    }

  implicit object JsResultInstances extends MonadError[JsResult, Throwable] {
    def pure[A](a: A): JsResult[A] =
      JsSuccess(a)

    def handleErrorWith[A](fa: JsResult[A])(f: Throwable => JsResult[A]): JsResult[A] =
      fa.recoverWith(f.compose(errorToException(_)))

    def raiseError[A](e: Throwable): JsResult[A] =
      JsError(e.getMessage)

    def flatMap[A, B](fa: JsResult[A])(f: A => JsResult[B]): JsResult[B] =
      fa.flatMap(f)

    @tailrec
    def tailRecM[A, B](a: A)(f: A => JsResult[Either[A, B]]): JsResult[B] =
      f(a) match {
        case e @ JsError(_)         => e
        case JsSuccess(Left(a), _)  => tailRecM(a)(f)
        case JsSuccess(Right(b), p) => JsSuccess(b, p)
      }

  }

  implicit val PointerFormat: Format[Pointer] =
    Format[Pointer](Reads {
      case JsString(s) => Pointer.parse[JsResult](s)
      case value       => JsError(f"Pointer expected: $value")
    }, Writes(p => JsString(p.show)))

  implicit val OperationFormat: Format[Operation[JsValue]] =
    Format[Operation[JsValue]](
      Reads {
        case obj @ play.api.libs.json.JsObject(fields) if fields.contains("op") =>
          fields("op") match {
            case JsString("add") =>
              (fields.get("path"), fields.get("value")) match {
                case (Some(JsString(path)), Some(value)) =>
                  Pointer.parse[JsResult](path).map(Add(_, value))
                case _ =>
                  JsError("missing 'path' or 'value' field")
              }
            case JsString("remove") =>
              (fields.get("path"), fields.get("old")) match {
                case (Some(JsString(path)), old) =>
                  Pointer.parse[JsResult](path).map(Remove(_, old))
                case _ =>
                  JsError("missing 'path' field")
              }
            case JsString("replace") =>
              (fields.get("path"), fields.get("value"), fields.get("old")) match {
                case (Some(JsString(path)), Some(value), old) =>
                  Pointer.parse[JsResult](path).map(Replace(_, value, old))
                case _ =>
                  JsError("missing 'path' or 'value' field")
              }
            case JsString("move") =>
              (fields.get("from"), fields.get("path")) match {
                case (Some(JsString(from)), Some(JsString(path))) =>
                  (Pointer.parse[JsResult](from), Pointer.parse[JsResult](path)).mapN(Move(_, _))
                case _ =>
                  JsError("missing 'from' or 'path' field")
              }
            case JsString("copy") =>
              (fields.get("from"), fields.get("path")) match {
                case (Some(JsString(from)), Some(JsString(path))) =>
                  (Pointer.parse[JsResult](from), Pointer.parse[JsResult](path)).mapN(Copy(_, _))
                case _ =>
                  JsError("missing 'from' or 'path' field")
              }
            case JsString("test") =>
              (fields.get("path"), fields.get("value")) match {
                case (Some(JsString(path)), Some(value)) =>
                  Pointer.parse[JsResult](path).map(Test(_, value))
                case _ =>
                  JsError("missing 'path' or 'value' field")
              }
            case op =>
              JsError(f"Unknown operation ${Json.stringify(op)}")
          }
        case value =>
          JsError(f"Operation[JsValue] expected: $value")
      },
      Writes {
        case Add(path, value) =>
          Json.obj("op" -> JsString("add"), "path" -> JsString(path.show), "value" -> value)
        case Remove(path, Some(old)) =>
          Json.obj("op" -> JsString("remove"), "path" -> JsString(path.show), "old" -> old)
        case Remove(path, None) =>
          Json.obj("op" -> JsString("remove"), "path" -> JsString(path.show))
        case Replace(path, value, Some(old)) =>
          Json.obj("op" -> JsString("replace"), "path" -> JsString(path.show), "value" -> value, "old" -> old)
        case Replace(path, value, None) =>
          Json.obj("op" -> JsString("replace"), "path" -> JsString(path.show), "value" -> value)
        case Move(from, path) =>
          Json.obj("op" -> JsString("move"), "from" -> JsString(from.show), "path" -> JsString(path.show))
        case Copy(from, path) =>
          Json.obj("op" -> JsString("copy"), "from" -> JsString(from.show), "path" -> JsString(path.show))
        case Test(path, value) =>
          Json.obj("op" -> JsString("test"), "path" -> JsString(path.show), "value" -> value)
      })

  implicit val JsonPatchFormat: Format[JsonPatch[JsValue]] =
    Format[JsonPatch[JsValue]](
      Reads[JsonPatch[JsValue]] { js =>
        js.validate[List[Operation[JsValue]]].map(JsonPatch(_)).recoverWith {
          case JsError(errors) => JsError((JsPath -> Seq(JsonValidationError("JsonPatch[JsValue] expected"))) +: errors)
        }
      },
      Writes(patch => play.api.libs.json.JsArray(patch.ops.map(Json.toJson(_)).toVector)))

  implicit val JsonMergePatchFormat: Format[JsonMergePatch[JsValue]] =
    Format[JsonMergePatch[JsValue]](
      Reads[JsonMergePatch[JsValue]] {
        case play.api.libs.json.JsObject(flds) => JsSuccess(JsonMergePatch.Object(flds.toMap))
        case value                             => JsSuccess(JsonMergePatch.Value(value))
      },
      Writes {
        case JsonMergePatch.Object(flds) => play.api.libs.json.JsObject(flds)
        case JsonMergePatch.Value(v)     => v
      })

}
