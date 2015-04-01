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

import schema._

import java.net.{
  URI,
  URISyntaxException
}

object DiffsonProtocol extends DefaultJsonProtocol {

  implicit object URIFormat extends JsonFormat[URI] {

    def write(uri: URI): JsString =
      JsString(uri.normalize.toString)

    def read(value: JsValue): URI = value match {
      case JsString(uri) =>
        try {
          new URI(uri)
        } catch {
          case e: URISyntaxException => deserializationError("Wrong URI format")
        }
      case _ =>
        deserializationError("URI expected")
    }
  }

  implicit val refForm = jsonFormat1(JsonReference)

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
        op.toJson

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
              obj.getFields("path") match {
                case Seq(JsString(path)) =>
                  Remove(pointer.parse(path))
                case _ =>
                  throw new FormatException("missing 'path' field")
              }
            case JsString("replace") =>
              obj.getFields("path", "value") match {
                case Seq(JsString(path), value) =>
                  Replace(pointer.parse(path), value)
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
        JsArray(patch.ops.toJson)

      def read(value: JsValue): JsonPatch = value match {
        case JsArray(ops) =>
          JsonPatch(ops.map(_.convertTo[Operation]).toList)
        case _ => throw new FormatException("JsonPatch expected")
      }
    }

  implicit object JsTypeFormat extends JsonFormat[JsType] {

    def write(tpe: JsType): JsValue = tpe match {
      case JsArrayType   => JsString("array")
      case JsBooleanType => JsString("boolean")
      case JsIntegerType => JsString("integer")
      case JsNumberType  => JsString("number")
      case JsNullType    => JsString("null")
      case JsObjectType  => JsString("object")
      case JsStringType  => JsString("string")
    }

    def read(json: JsValue): JsType = json match {
      case JsString("array")   => JsArrayType
      case JsString("boolean") => JsBooleanType
      case JsString("integer") => JsIntegerType
      case JsString("number")  => JsNumberType
      case JsString("null")    => JsNullType
      case JsString("object")  => JsObjectType
      case JsString("string")  => JsStringType
      case _                   => deserializationError("json type string expected")
    }

  }

  implicit object JsonSchemaFormat extends RootJsonFormat[JsonSchema] {

    implicit val numericSchemaFormat = jsonFormat5(NumericSchema)

    implicit val stringSchemaFormat = jsonFormat3(StringSchema)

    implicit val arraySchemaFormat = jsonFormat5(ArraySchema)

    implicit val objectSchemaFormat = jsonFormat7(ObjectSchema)

    implicit val anySchemaFormat = jsonFormat7(AnySchema)

    implicit val metadataSchemaFormat = jsonFormat3(MetadataSchema)

    def write(schema: JsonSchema): JsObject = schema match {
      case JsonSchema(sc, id, subs, nums, strs, arrs, objs, any, meta) =>
        val numerics = nums.toJson.asJsObject.fields
        val strings = strs.toJson.asJsObject.fields
        val arrays = arrs.toJson.asJsObject.fields
        val objects = objs.toJson.asJsObject.fields
        val anys = any.toJson.asJsObject.fields
        val metadata = meta.toJson.asJsObject.fields
        val subschemas = subs.toJson.asJsObject.fields
        JsObject(numerics
          ++ strings
          ++ arrays
          ++ objects
          ++ anys
          ++ metadata
          ++ subschemas
          + ("$schema" -> sc.toJson)
          + ("id" -> id.toJson))
    }

    def read(value: JsValue): JsonSchema = {
      val (schema, id, subs) = value match {
        case JsObject(fields) =>
          (fields.get("$schema").map(_.convertTo[URI]),
            fields.get("id").map(_.convertTo[URI]),
            fields.filter(!_._1.isKeyword).map {
              case (k, v) => (k, v.convertTo[JsonSchema])
            })
        case _ =>
          deserializationError("json schema expected")
      }
      val numerics = value.convertTo[NumericSchema]
      val strings = value.convertTo[StringSchema]
      val arrays = value.convertTo[ArraySchema]
      val objects = value.convertTo[ObjectSchema]
      val anys = value.convertTo[AnySchema]
      val metadata = value.convertTo[MetadataSchema]
      JsonSchema(schema, id, subs, numerics, strings, arrays, objects, anys, metadata)
    }
  }

}
