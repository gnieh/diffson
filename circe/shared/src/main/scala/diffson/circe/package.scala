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
package diffson

import jsonpatch._
import jsonpointer._
import jsonmergepatch._

import cats.{ Apply, FlatMap }
import cats.syntax.all._
import io.circe._
import io.circe.Decoder.Result
import io.circe.syntax._

package object circe {

  implicit object jsonyCirce extends Jsony[Json] {

    def Null: Json = Json.Null

    def array(json: Json): Option[Vector[Json]] =
      json.asArray

    def fields(json: Json): Option[Map[String, Json]] =
      json.asObject.map(_.toMap)

    def makeArray(values: Vector[Json]): Json =
      Json.fromValues(values)

    def makeObject(fields: Map[String, Json]): Json =
      Json.fromFields(fields)

    def show(json: Json): String =
      json.noSpaces

    def eqv(json1: Json, json2: Json) =
      Json.eqJson.eqv(json1, json2)

  }

  implicit val pointerEncoder: Encoder[Pointer] =
    Encoder[String].contramap(_.show)

  implicit val pointerDecoder: Decoder[Pointer] =
    Decoder[String].emap(Pointer.parse[Either[Throwable, ?]](_).leftMap(_.getMessage))

  implicit val operationEncoder: Encoder[Operation[Json]] =
    Encoder.instance[Operation[Json]] {
      case Add(path, value) =>
        Json.obj(
          "op" -> Json.fromString("add"),
          "path" -> Json.fromString(path.show),
          "value" -> value)
      case Remove(path, Some(old)) =>
        Json.obj(
          "op" -> Json.fromString("remove"),
          "path" -> Json.fromString(path.show),
          "old" -> old)
      case Remove(path, None) =>
        Json.obj(
          "op" -> Json.fromString("remove"),
          "path" -> Json.fromString(path.show))
      case Replace(path, value, Some(old)) =>
        Json.obj(
          "op" -> Json.fromString("replace"),
          "path" -> Json.fromString(path.show),
          "value" -> value,
          "old" -> old)
      case Replace(path, value, None) =>
        Json.obj(
          "op" -> Json.fromString("replace"),
          "path" -> Json.fromString(path.show),
          "value" -> value)
      case Move(from, path) =>
        Json.obj(
          "op" -> Json.fromString("move"),
          "from" -> Json.fromString(from.show),
          "path" -> Json.fromString(path.show))
      case Copy(from, path) =>
        Json.obj(
          "op" -> Json.fromString("copy"),
          "from" -> Json.fromString(from.show),
          "path" -> Json.fromString(path.show))
      case Test(path, value) =>
        Json.obj(
          "op" -> Json.fromString("test"),
          "path" -> Json.fromString(path.show),
          "value" -> value)
    }

  implicit val operationDecoder: Decoder[Operation[Json]] =
    new Decoder[Operation[Json]] {

      private val A = Apply[Result]
      private val F = FlatMap[Result]

      override def apply(c: HCursor): Result[Operation[Json]] =
        F.flatMap(c.get[String]("op").leftMap(_.copy(message = "missing 'op' field"))) {
          case "add" =>
            A.map2(c.get[Pointer]("path"), c.get[Json]("value"))(Add[Json])
              .leftMap(_.copy(message = "missing 'path' or 'value' field"))
          case "remove" =>
            A.map2(c.get[Pointer]("path"), c.get[Option[Json]]("old"))(Remove[Json])
              .leftMap(_.copy(message = "missing 'path' field"))
          case "replace" =>
            A.map3(c.get[Pointer]("path"), c.get[Json]("value"), c.get[Option[Json]]("old"))(Replace[Json] _)
              .leftMap(_.copy(message = "missing 'path' or 'value' field"))
          case "move" =>
            A.map2(c.get[Pointer]("from"), c.get[Pointer]("path"))(Move[Json])
              .leftMap(_.copy(message = "missing 'from' or 'path' field"))
          case "copy" =>
            A.map2(c.get[Pointer]("from"), c.get[Pointer]("path"))(Copy[Json])
              .leftMap(_.copy(message = "missing 'from' or 'path' field"))
          case "test" =>
            A.map2(c.get[Pointer]("path"), c.get[Json]("value"))(Test[Json])
              .leftMap(_.copy(message = "missing 'path' or 'value' field"))
          case other =>
            Left(DecodingFailure(s"""Unknown operation "$other"""", c.history))
        }
    }

  implicit val jsonPatchEncoder: Encoder[JsonPatch[Json]] =
    Encoder[List[Json]].contramap(_.ops.map(_.asJson))

  implicit val jsonPatchDecoder: Decoder[JsonPatch[Json]] =
    new Decoder[JsonPatch[Json]] {

      private val F = FlatMap[Result]

      override def apply(c: HCursor): Result[JsonPatch[Json]] =
        F.flatMap(c.as[List[Json]]) { list =>
          F.map(list.traverse(_.as[Operation[Json]]))(JsonPatch(_))
        }
    }

  implicit val jsonMergePatchEncoder: Encoder[JsonMergePatch[Json]] =
    Encoder.instance[JsonMergePatch[Json]](_.toJson)

  implicit val jsonMergePatchDecoder: Decoder[JsonMergePatch[Json]] =
    new Decoder[JsonMergePatch[Json]] {
      override def apply(c: HCursor): Result[JsonMergePatch[Json]] =
        c.value.asObject match {
          case Some(obj) => Right(JsonMergePatch.Object(obj.toMap))
          case None      => Right(JsonMergePatch.Value(c.value))
        }
    }
}
