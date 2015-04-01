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

  def validate(json: JsValue): Boolean

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

  def validate(json: JsValue): Boolean =
    numericKeywords.validate(json) &&
      stringKewords.validate(json) &&
      arrayKeywords.validate(json) &&
      objectKeywords.validate(json) &&
      anyKeywords.validate(json)

}

case class NumericSchema(multipleOf: Option[BigDecimal],
    maximum: Option[BigDecimal],
    exclusiveMaximum: Boolean,
    minimum: Option[BigDecimal],
    exclusiveMinimum: Boolean) {

  def validate(json: JsValue): Boolean = json match {
    case JsNumber(num) =>
      val mult = multipleOf.fold(true) { mul =>
        num % mul == BigDecimal(0)
      }
      def max = maximum.fold(true) { max =>
        if (exclusiveMaximum)
          num < max
        else
          num <= max
      }
      def min = minimum.fold(true) { min =>
        if (exclusiveMinimum)
          num > min
        else
          num >= min
      }
      mult && max && min
    case _ =>
      true
  }

}

case class StringSchema(maxLength: Option[Int],
    minLength: Int,
    pattern: Option[String]) {

  def validate(json: JsValue): Boolean = json match {
    case JsString(str) =>
      def max = maxLength.fold(true) { max =>
        str.length <= max
      }
      def min = str.length >= minLength
      def pat = pattern.fold(true) { pat =>
        str.matches(pat)
      }
      max && min && pat
    case _ =>
      true
  }

}

case class ArraySchema(items: Either[JsonSchema, List[JsonSchema]],
    additionalItems: Either[Boolean, JsonSchema],
    maxItems: Option[Int],
    minItems: Int,
    uniqueItems: Boolean) {

  def validate(json: JsValue): Boolean = json match {
    case JsArray(elems) =>
      def max = maxItems.fold(true) { max =>
        elems.size <= max
      }
      def min = elems.size <= minItems
      def dist =
        if (uniqueItems)
          elems.distinct == elems
        else
          true
      def it = items match {
        case Left(schema) =>
          // all elements must validate this schema
          elems.forall(elem => schema.validate(elem))
        case Right(schemas) =>
          elems.zipWithIndex.forall {
            case (elem, idx) =>
              if (idx < schemas.size)
                schemas(idx).validate(elem)
              else additionalItems match {
                case Left(ok) =>
                  ok
                case Right(schema) =>
                  schema.validate(elem)
              }
          }
      }
      max && min && dist && it
    case _ =>
      true
  }

}

case class ObjectSchema(maxProperties: Option[Int],
    minProperties: Int,
    required: Option[Set[String]],
    additionalProperties: Either[Boolean, JsonSchema],
    properties: Map[String, JsonSchema],
    patternProperties: Map[String, JsonSchema],
    dependencies: Map[String, Either[JsonSchema, Set[String]]]) {

  def validate(json: JsValue): Boolean = json match {
    case JsObject(fields) =>
      def max = maxProperties.fold(true) { max =>
        fields.size <= max
      }
      def min = fields.size >= minProperties
      def req = required.fold(true) { req =>
        req.forall(n => fields.contains(n))
      }
      def props = fields.forall {
        case (name, value) =>
          val (p, inp) =
            if (properties.contains(name))
              (properties(name).validate(value), true)
            else
              (true, false)
          val (pp, inpp) =
            patternProperties.foldLeft((true, false)) {
              case ((ok, inpp), (ppn, pps)) =>
                if (name.matches(ppn))
                  (ok && pps.validate(value), true)
                else
                  (ok, inpp)
            }
          val add =
            if (inp || inpp) {
              true
            } else additionalProperties match {
              case Left(ok)      => ok
              case Right(schema) => schema.validate(value)
            }
          p && pp && add
      }
      def deps = fields.keys.forall { name =>
        dependencies.get(name).fold(true) {
          case Left(schema) => schema.validate(json)
          case Right(names) => names.forall(n => fields.contains(n))
        }
      }
      max && min && req && deps
    case _ =>
      true
  }

}

case class AnySchema(enum: Option[List[JsValue]],
    `type`: Option[JsType],
    allOf: Option[List[JsonSchema]],
    anyOf: Option[List[JsonSchema]],
    oneOf: Option[List[JsonSchema]],
    not: Option[JsonSchema],
    definitions: Map[String, JsonSchema]) {

  def validate(json: JsValue): Boolean = {
    def en = enum.fold(true) { en =>
      en.contains(json)
    }
    def tpe = `type`.fold(true) { tpe =>
      tpe.validate(json)
    }
    def all = allOf.fold(true) { all =>
      all.forall(s => s.validate(json))
    }
    def one = oneOf.fold(true) { one =>
      one.exists(s => s.validate(json))
    }
    def n = not.fold(true) { s =>
      !s.validate(json)
    }
    en && tpe && all && one && n
  }

}

case class MetadataSchema(title: Option[String],
  description: Option[String],
  default: Option[JsValue])
