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
package schema

import java.net.URI

import spray.json._

import scala.util.matching.Regex

sealed trait JsonSchema {

  val $schema: Option[URI]
  val id: Option[URI]
  val subschemas: Map[String, JsonSchema]
  val numericKeywords: NumericSchema
  val stringKewords: StringSchema
  val arrayKeywords: ArraySchema
  val objectKeywords: ObjectSchema
  val anyKeywords: AnySchema
  val metadataKeywords: MetadataSchema

  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError]

}

object JsonSchema {

  def apply($schema: Option[URI],
    id: Option[URI],
    subschemas: Map[String, JsonSchema],
    numericKeywords: NumericSchema,
    stringKewords: StringSchema,
    arrayKeywords: ArraySchema,
    objectKeywords: ObjectSchema,
    anyKeywords: AnySchema,
    metadataKeywords: MetadataSchema): JsonSchema =
    new ResolvedJsonSchema($schema,
      id,
      subschemas,
      numericKeywords,
      stringKewords,
      arrayKeywords,
      objectKeywords,
      anyKeywords,
      metadataKeywords)

  def unapply(schema: JsonSchema) =
    Some((schema.$schema,
      schema.id,
      schema.subschemas,
      schema.numericKeywords,
      schema.stringKewords,
      schema.arrayKeywords,
      schema.objectKeywords,
      schema.anyKeywords,
      schema.metadataKeywords))

}

private class RefJsonSchema(val $ref: URI)

private class ResolvedJsonSchema(val $schema: Option[URI],
    val id: Option[URI],
    val subschemas: Map[String, JsonSchema],
    val numericKeywords: NumericSchema,
    val stringKewords: StringSchema,
    val arrayKeywords: ArraySchema,
    val objectKeywords: ObjectSchema,
    val anyKeywords: AnySchema,
    val metadataKeywords: MetadataSchema) extends JsonSchema {

  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] =
    numericKeywords.validate(pointer, json) ++
      stringKewords.validate(pointer, json) ++
      arrayKeywords.validate(pointer, json) ++
      objectKeywords.validate(pointer, json) ++
      anyKeywords.validate(pointer, json)

}

case class NumericSchema(multipleOf: Option[BigDecimal],
    maximum: Option[BigDecimal],
    exclusiveMaximum: Boolean,
    minimum: Option[BigDecimal],
    exclusiveMinimum: Boolean) {

  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsNumber(num) =>
      val mult = multipleOf.fold(noError) { mul =>
        validateInner(num % mul == BigDecimal(0), pointer, f"Multiple of $mul expected but got $num")
      }
      def max = maximum.fold(noError) { max =>
        if (exclusiveMaximum)
          validateInner(num < max, pointer, f"Maximum of $max (exclusive) expected but got $num")
        else
          validateInner(num <= max, pointer, f"Maximum of $max (inclusive) expected but got $num")
      }
      def min = minimum.fold(noError) { min =>
        if (exclusiveMinimum)
          validateInner(num > min, pointer, f"Minimum of $max (exclusive) expected but got $num")
        else
          validateInner(num >= min, pointer, f"Minimum of $max (exclusive) expected but got $num")
      }
      mult ++ max ++ min
    case _ =>
      noError
  }

}

case class StringSchema(maxLength: Option[Int],
    minLength: Int,
    pattern: Option[String]) {

  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsString(str) =>
      def max = maxLength.fold(noError) { max =>
        validateInner(str.length <= max, pointer, f"String of maximum length $max expected but got string of length ${str.length}")
      }
      def min =
        validateInner(str.length >= minLength, pointer, f"String of minimum length $minLength expected but got string of length ${str.length}")
      def pat = pattern.fold(noError) { pat =>
        validateInner(str.matches(pat), pointer, f"String matching pattern `$pat` expected")
      }
      max ++ min ++ pat
    case _ =>
      noError
  }

}

case class ArraySchema(items: Either[JsonSchema, Vector[JsonSchema]],
    additionalItems: Either[Boolean, JsonSchema],
    maxItems: Option[Int],
    minItems: Int,
    uniqueItems: Boolean) {

  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsArray(elems) =>
      def max = maxItems.fold(noError) { max =>
        validateInner(elems.size <= max, pointer, f"Array of maximum length $max expected but gor array of length ${elems.size}")
      }
      def min =
        validateInner(elems.size >= minItems, pointer, f"Array of minimum length $minItems expected but got array of length ${elems.length}")
      def dist =
        if (uniqueItems)
          validateInner(elems.distinct == elems, pointer, "Array of distinct elements expected")
        else
          noError
      def it = items match {
        case Left(schema) =>
          // all elements must validate this schema
          elems.zipWithIndex.flatMap {
            case (elem, idx) => schema.validate(pointer / idx, elem)
          }
        case Right(schemas) =>
          elems.zipWithIndex.flatMap {
            case (elem, idx) =>
              if (idx < schemas.size)
                schemas(idx).validate(pointer / idx, elem)
              else additionalItems match {
                case Left(true) =>
                  noError
                case Left(false) =>
                  Vector(ValidationError(pointer / idx, f"No additional elements authorized"))
                case Right(schema) =>
                  schema.validate(pointer / idx, elem)
              }
          }
      }
      max ++ min ++ dist ++ it
    case _ =>
      noError
  }

}

case class ObjectSchema(maxProperties: Option[Int],
    minProperties: Int,
    required: Option[Set[String]],
    additionalProperties: Either[Boolean, JsonSchema],
    properties: Map[String, JsonSchema],
    patternProperties: Map[String, JsonSchema],
    dependencies: Map[String, Either[JsonSchema, Set[String]]]) {

  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsObject(fields) =>
      def max = maxProperties.fold(noError) { max =>
        validateInner(fields.size <= max, pointer, f"Expected object with maximum field number $max but got an object with ${fields.size} field(s)")
      }
      def min =
        validateInner(fields.size >= minProperties, pointer, f"Expected object with minimum field number $minProperties but got an object with ${fields.size} field(s)")
      def req = required.fold(noError) { req =>
        req.toVector.flatMap(n => validateInner(fields.contains(n), pointer, f"Missing required field $n"))
      }
      def props = fields.flatMap {
        case (name, value) =>
          // (validation errors for "properties", indicate whether this field is in "properties")
          val (p, inp) =
            if (properties.contains(name))
              (properties(name).validate(pointer / name, value), true)
            else
              (noError, false)
          // (validation errors for "patternProperties", indicate whether this field is in "patternProperties")
          val (pp, inpp) =
            patternProperties.foldLeft((noError, false)) {
              case ((err, inpp), (ppn, pps)) =>
                if (name.matches(ppn))
                  (err ++ pps.validate(pointer / name, value), true)
                else
                  (err, inpp)
            }
          val add =
            if (inp || inpp) {
              noError
            } else additionalProperties match {
              case Left(true)    => noError
              case Left(false)   => Vector(ValidationError(pointer / name, "No additional properties are allowed"))
              case Right(schema) => schema.validate(pointer / name, value)
            }
          p ++ pp ++ add
      }
      def deps = fields.keys.flatMap { name =>
        dependencies.get(name).fold(noError) {
          case Left(schema) => schema.validate(pointer, json)
          case Right(names) => names.toVector.flatMap(n => validateInner(fields.contains(n), pointer, f"Missing required dependent field $n (because of presence of field $name)"))
        }
      }
      max ++ min ++ req ++ deps
    case _ =>
      noError
  }

}

case class AnySchema(enum: Option[Vector[JsValue]],
    `type`: Option[JsType],
    allOf: Option[Vector[JsonSchema]],
    anyOf: Option[Vector[JsonSchema]],
    oneOf: Option[Vector[JsonSchema]],
    not: Option[JsonSchema],
    definitions: Map[String, JsonSchema]) {

  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = {
    def en = enum.fold(noError) { en =>
      validateInner(en.contains(json), pointer, f"Unexpected value")
    }
    def tpe = `type`.fold(noError) { tpe =>
      tpe.validate(pointer, json)
    }
    def all = allOf.fold(noError) { all =>
      all.flatMap(s => s.validate(pointer, json))
    }
    def one = oneOf.fold(noError) { one =>
      if (one.exists(s => s.validate(pointer, json).isEmpty))
        noError
      else
        Vector(ValidationError(pointer, "Expected to validate one schema in the list"))
    }
    def n = not.fold(noError) { s =>
      if (s.validate(pointer, json).isEmpty)
        Vector(ValidationError(pointer, "Not expected to validate"))
      else
        noError
    }
    en ++ tpe ++ all ++ one ++ n
  }

}

case class MetadataSchema(title: Option[String],
  description: Option[String],
  default: Option[JsValue])
