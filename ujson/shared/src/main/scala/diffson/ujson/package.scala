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

import cats.implicits._
import diffson.jsonmergepatch.JsonMergePatch
import diffson.jsonpatch._
import diffson.jsonpointer.Pointer
import _root_.ujson.{Value, Arr, Obj, Str}
import upickle.default._

import scala.util.Try

package object ujson {

  implicit val jsonyUjson: Jsony[Value] = new Jsony[Value] {

    def Null: Value = _root_.ujson.Null

    def array(json: Value): Option[Vector[Value]] =
      json.arrOpt.map(_.toVector)

    def fields(json: Value): Option[Map[String, Value]] =
      json.objOpt.map(_.toMap)

    def makeArray(values: Vector[Value]): Value =
      Arr.from(values)

    def makeObject(fields: Map[String, Value]): Value =
      Obj.from(fields)

    def show(json: Value): String =
      json.render()

    def eqv(json1: Value, json2: Value): Boolean = json1 == json2
  }

  implicit val pointerEncoder: Writer[Pointer] =
    implicitly[Writer[String]].comap(a => a.show)

  implicit val pointerDecoder: Reader[Pointer] =
    implicitly[Reader[String]].map { str =>
      Pointer.parse[Try](str).fold(throw _, identity)
    }

  private val operationAsJson: Operation[Value] => Value = {
    case Add(path, value) =>
      Obj(
        "op" -> Str("add"),
        "path" -> Str(path.show),
        "value" -> value
      )
    case Remove(path, Some(old)) =>
      Obj(
        "op" -> Str("remove"),
        "path" -> Str(path.show),
        "old" -> old
      )
    case Remove(path, None) =>
      Obj(
        "op" -> Str("remove"),
        "path" -> Str(path.show)
      )
    case Replace(path, value, Some(old)) =>
      Obj(
        "op" -> Str("replace"),
        "path" -> Str(path.show),
        "value" -> value,
        "old" -> old
      )
    case Replace(path, value, None) =>
      Obj(
        "op" -> Str("replace"),
        "path" -> Str(path.show),
        "value" -> value
      )
    case Move(from, path) =>
      Obj(
        "op" -> Str("move"),
        "from" -> Str(from.show),
        "path" -> Str(path.show)
      )
    case Copy(from, path) =>
      Obj(
        "op" -> Str("copy"),
        "from" -> Str(from.show),
        "path" -> Str(path.show)
      )
    case Test(path, value) =>
      Obj(
        "op" -> Str("test"),
        "path" -> Str(path.show),
        "value" -> value
      )
  }

  implicit val operationEncoder: Writer[Operation[Value]] =
    implicitly[Writer[Value]].comap(operationAsJson)

  private def decodeOperation(value: Value): Operation[Value] = {
    def readPointer(value: Value, path: String = "path"): Pointer = {
      val serializedPointer =
        value.objOpt
          .flatMap(_.get(path))
          .getOrElse(throw FieldMissing(path))

      read[Pointer](serializedPointer)
    }

    val op =
      value.objOpt
        .flatMap(_.get("op").flatMap(_.strOpt))
        .getOrElse(throw FieldMissing("op"))

    def getField(key: String) = {
      value.objOpt
        .flatMap(_.get(key))
        .getOrElse(throw FieldMissing(key))
    }

    op match {
      case "add" =>
        val path = readPointer(value)
        val v = getField("value")
        Add(path, v)

      case "remove" =>
        val path = readPointer(value)
        val old = value.objOpt.flatMap(_.get("old"))
        Remove(path, old)
      case "replace" =>
        val path = readPointer(value)
        val payload = getField("value")
        val old = value.objOpt.flatMap(_.get("old"))
        Replace(path, payload, old)
      case "move" =>
        val path = readPointer(value)
        val from = readPointer(value, "from")
        Move(from, path)
      case "copy" =>
        val path = readPointer(value)
        val from = readPointer(value, "from")
        Copy(from, path)
      case "test" =>
        val path = readPointer(value)
        val payload = getField("value")
        Test(path, payload)
      case other =>
        throw new Exception(s"Expected operation `$other` but it is missing")
    }
  }

  implicit val operationDecoder: Reader[Operation[Value]] =
    implicitly[Reader[Value]].map(decodeOperation)

  implicit val jsonPatchEncoder: Writer[JsonPatch[Value]] = {
    implicitly[Writer[List[Value]]].comap(_.ops.map(operationAsJson))
  }

  implicit val jsonPatchDecoder: Reader[JsonPatch[Value]] = {
    implicitly[Reader[List[Value]]].map(q => JsonPatch(q.map(decodeOperation)))
  }

  implicit val jsonMergePatchEncoder: Writer[JsonMergePatch[Value]] = {
    implicitly[Writer[Value]].comap(_.toJson)
  }

  implicit val jsonMergePatchDecoder: Reader[JsonMergePatch[Value]] = {
    implicitly[Reader[Value]]
      .map { value =>
        value.objOpt
          .map(obj => JsonMergePatch.Object(obj.toMap))
          .getOrElse(JsonMergePatch.Value(value))
      }
  }
}
