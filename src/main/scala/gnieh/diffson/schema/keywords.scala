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

import scala.util.matching.Regex

import spray.json._

import DiffsonProtocol._

sealed trait Keyword {
  def toJson: JsField
}

object Keyword {
  def all = Map[String, JsValue => Keyword](
    "multipleOf" -> MultipleOf,
    "maximum" -> Maximum(false),
    "exclusiveMaximum" -> Maximum(true),
    "minimum" -> Minimum(false),
    "exclusiveMinimum" -> Minimum(true),
    "maxLength" -> MaxLength,
    "minLength" -> MinLength,
    "pattern" -> Pattern,
    "items" -> Items,
    "additionalItems" -> AdditionalItems,
    "maxItems" -> MaxItems,
    "minItems" -> MinItems,
    "uniqueItems" -> UniqueItems,
    "maxProperties" -> MaxProperties,
    "minProperties" -> MinProperties,
    "required" -> Required,
    "additionalProperties" -> AdditionalProperties,
    "properties" -> Properties,
    "patternProperties" -> PatternProperties,
    "dependencies" -> Dependencies,
    "enum" -> Enum,
    "type" -> Type,
    "allOf" -> AllOf,
    "anyOf" -> AnyOf,
    "oneOf" -> OneOf,
    "not" -> Not,
    "definitions" -> Definitions,
    "title" -> Title,
    "description" -> Description,
    "default" -> Default)

}

sealed trait NumericKeyword extends Keyword

sealed trait StringKeyword extends Keyword

sealed trait ArrayKeyword extends Keyword

sealed trait ObjectKeyword extends Keyword

sealed trait AnyKeyword extends Keyword

sealed trait MetadataKeyword extends Keyword

case class MultipleOf(n: BigDecimal) extends NumericKeyword {
  def toJson = "multipleOf" -> n.toJson
}
object MultipleOf extends (JsValue => MultipleOf) {
  def apply(json: JsValue) = json match {
    case JsNumber(n) => MultipleOf(n)
    case _           => deserializationError("number expected")
  }
}

case class Maximum(n: BigDecimal, exclusive: Boolean) extends NumericKeyword {
  def toJson =
    if(exclusive)
      "exclusiveMaximum" -> n.toJson
    else
      "maximum" -> n.toJson
}
object Maximum {
  def apply(exclusive: Boolean)(json: JsValue): Maximum = json match {
    case JsNumber(n) => Maximum(n, exclusive)
    case _           => deserializationError("number expected")
  }
}

case class Minimum(n: BigDecimal, exclusive: Boolean) extends NumericKeyword {
  def toJson =
    if(exclusive)
      "exclusiveMinimum" -> n.toJson
    else
      "minimum" -> n.toJson
}
object Minimum {
  def apply(exclusive: Boolean)(json: JsValue): Minimum = json match {
    case JsNumber(n) => Minimum(n, exclusive)
    case _           => deserializationError("number expected")
  }
}

case class MaxLength(n: Int) extends StringKeyword {
  def toJson = "maxLength" -> n.toJson
}
object MaxLength extends (JsValue => MaxLength) {
  def apply(json: JsValue) = json match {
    case JsNumber(n) if n.isValidInt && n >= 0 => MaxLength(n.intValue)
    case _                                     => deserializationError("positive integer expected")
  }
}

case class MinLength(n: Int) extends StringKeyword {
  def toJson = "minLength" -> n.toJson
}
object MinLength extends (JsValue => MinLength) {
  def apply(json: JsValue) = json match {
    case JsNumber(n) if n.isValidInt && n >= 0 => MinLength(n.intValue)
    case _                                     => deserializationError("positive integer expected")
  }
}

case class Pattern(p: Regex) extends StringKeyword {
  def toJson = "pattern" -> p.regex.toJson
}
object Pattern extends (JsValue => Pattern) {
  def apply(json: JsValue) = json match {
    case JsString(s) =>
      try {
        Pattern(s.r)
      } catch {
        case e: Exception => deserializationError("ECMA 262 pattern expected")
      }
    case _           => deserializationError("ECMA 262 pattern expected")
  }
}

case class Items(i: Either[JsonSchema,List[JsonSchema]]) extends ArrayKeyword {
  def toJson = "items" -> i.toJson
}
object Items extends (JsValue => Items) {
  def apply(json: JsValue) = json match {
    case obj @ JsObject(_)  => Items(Left(obj.convertTo[JsonSchema]))
    case array @ JsArray(_) => Items(Right(array.convertTo[List[JsonSchema]]))
    case _                  => deserializationError("schema or array of schemas expected")
  }
}

case class AdditionalItems(i: Either[Boolean, JsonSchema]) extends ArrayKeyword {
  def toJson = "additionalItems" -> i.toJson
}
object AdditionalItems extends (JsValue => AdditionalItems) {
  def apply(json: JsValue) = json match {
    case JsBoolean(b)       => AdditionalItems(Left(b))
    case obj @ JsObject(_)  => AdditionalItems(Right(obj.convertTo[JsonSchema]))
    case _                  => deserializationError("schema or boolean expected")
  }
}

case class MaxItems(n: Int) extends ArrayKeyword {
  def toJson = "maxItems" -> n.toJson
}
object MaxItems extends (JsValue => MaxItems) {
  def apply(json: JsValue) = json match {
    case JsNumber(n) if n.isValidInt && n >= 0 => MaxItems(n.intValue)
    case _                                     => deserializationError("positive integer expected")
  }
}

case class MinItems(n: Int) extends ArrayKeyword {
  def toJson = "minItems" -> n.toJson
}
object MinItems extends (JsValue => MinItems) {
  def apply(json: JsValue) = json match {
    case JsNumber(n) if n.isValidInt && n >= 0 => MinItems(n.intValue)
    case _                                     => deserializationError("positive integer expected")
  }
}

case class UniqueItems(b: Boolean) extends ArrayKeyword {
  def toJson = "uniqueItems" -> b.toJson
}
object UniqueItems extends (JsValue => UniqueItems) {
  def apply(json: JsValue) = json match {
    case JsBoolean(b) => UniqueItems(b)
    case _            => deserializationError("boolean expected")
  }
}

case class MaxProperties(n: Int) extends ObjectKeyword {
  def toJson = "maxProperties" -> n.toJson
}
object MaxProperties extends (JsValue => MaxProperties) {
  def apply(json: JsValue) = json match {
    case JsNumber(n) if n.isValidInt && n >= 0 => MaxProperties(n.intValue)
    case _                                     => deserializationError("positive integer expected")
  }
}

case class MinProperties(n: Int) extends ObjectKeyword {
  def toJson = "minProperties" -> n.toJson
}
object MinProperties extends (JsValue => MinProperties) {
  def apply(json: JsValue) = json match {
    case JsNumber(n) if n.isValidInt && n >= 0 => MinProperties(n.intValue)
    case _                                     => deserializationError("positive integer expected")
  }
}

case class Required(s: Set[String]) extends ObjectKeyword {
  def toJson = "required" -> s.toJson
}
object Required extends (JsValue => Required) {
  def apply(json: JsValue) = json match {
    case arr @ JsArray(f) if f.distinct == f => Required(arr.convertTo[Set[String]])
    case _                                   => deserializationError("array of unique string expected")
  }
}

case class AdditionalProperties(i: Either[Boolean, JsonSchema]) extends ObjectKeyword {
  def toJson = "additionalProperties" -> i.toJson
}
object AdditionalProperties extends (JsValue => AdditionalProperties) {
  def apply(json: JsValue) = json match {
    case JsBoolean(b)       => AdditionalProperties(Left(b))
    case obj @ JsObject(_)  => AdditionalProperties(Right(obj.convertTo[JsonSchema]))
    case _                  => deserializationError("schema or boolean expected")
  }
}

case class Properties(m: Map[String, JsonSchema]) extends ObjectKeyword {
  def toJson = "properties" -> m.toJson
}
object Properties extends (JsValue => Properties) {
  def apply(json: JsValue) = json match {
    case obj @ JsObject(_)  => Properties(obj.convertTo[Map[String, JsonSchema]])
    case _                  => deserializationError("object of schemas expected")
  }
}

case class PatternProperties(m: Map[String, JsonSchema]) extends ObjectKeyword {
  def toJson = "patternProperties" -> m.toJson
}
object PatternProperties extends (JsValue => PatternProperties) {
  def apply(json: JsValue) = json match {
    case obj @ JsObject(_)  => PatternProperties(obj.convertTo[Map[String, JsonSchema]])
    case _                  => deserializationError("object of schemas expected")
  }
}

case class Dependencies(m: Map[String, Either[JsonSchema,List[String]]]) extends ObjectKeyword {
  def toJson = "dependencies" -> m.toJson
}
object Dependencies extends (JsValue => Dependencies) {
  def apply(json: JsValue) = json match {
    case JsObject(fields) =>
      Dependencies(for((name, value) <- fields)
        yield (name, value match {
          case obj @ JsObject(_) => Left(obj.convertTo[JsonSchema])
          case arr @ JsArray(_)  => Right(arr.convertTo[List[String]])
          case _                 => deserializationError("schema or array of string expected")
        }))
    case _ => deserializationError("object of schemas expected")
  }
}

case class Enum(values: Set[JsValue]) extends AnyKeyword {
  def toJson = "enum" -> values.toJson
}
object Enum extends (JsValue => Enum) {
  def apply(json: JsValue) = json match {
    case arr @ JsArray(f) if f.distinct == f => Enum(arr.convertTo[Set[JsValue]])
    case _                                   => deserializationError("array of unique string expected")
  }
}

case class Type(tpe: JsType) extends AnyKeyword {
  def toJson = "type" -> tpe.toJson
}
object Type extends (JsValue => Type) {
  def apply(json: JsValue) = json match {
    case JsString("array")   => Type(JsArrayType)
    case JsString("boolean") => Type(JsBooleanType)
    case JsString("integer") => Type(JsIntegerType)
    case JsString("number")  => Type(JsNumberType)
    case JsString("null")    => Type(JsNullType)
    case JsString("object")  => Type(JsObjectType)
    case JsString("string")  => Type(JsStringType)
    case _                   => deserializationError("type expected")
  }
}

case class AllOf(l: List[JsonSchema]) extends AnyKeyword {
  def toJson = "allOf" -> l.toJson
}
object AllOf extends (JsValue => AllOf) {
  def apply(json: JsValue) = json match {
    case arr @ JsArray(_) => AllOf(arr.convertTo[List[JsonSchema]])
    case _                => deserializationError("array of schemas expected")
  }
}

case class AnyOf(l: List[JsonSchema]) extends AnyKeyword {
  def toJson = "anyOf" -> l.toJson
}
object AnyOf extends (JsValue => AnyOf) {
  def apply(json: JsValue) = json match {
    case arr @ JsArray(_) => AnyOf(arr.convertTo[List[JsonSchema]])
    case _                => deserializationError("array of schemas expected")
  }
}

case class OneOf(l: List[JsonSchema]) extends AnyKeyword {
  def toJson = "oneOf" -> l.toJson
}
object OneOf extends (JsValue => OneOf) {
  def apply(json: JsValue) = json match {
    case arr @ JsArray(_) => OneOf(arr.convertTo[List[JsonSchema]])
    case _                => deserializationError("array of schemas expected")
  }
}

case class Not(s: JsonSchema) extends AnyKeyword {
  def toJson = "not" -> s.toJson
}
object Not extends (JsValue => Not) {
  def apply(json: JsValue) = json match {
    case obj @ JsObject(_) => Not(obj.convertTo[JsonSchema])
    case _                 => deserializationError("schema expected")
  }
}

case class Definitions(s: Map[String,JsonSchema]) extends AnyKeyword {
  def toJson = "definitions" -> s.toJson
}
object Definitions extends (JsValue => Definitions) {
  def apply(json: JsValue) = json match {
    case obj @ JsObject(_) => Definitions(obj.convertTo[Map[String, JsonSchema]])
    case _                 => deserializationError("object of schemas expected")
  }
}

case class Title(s: String) extends MetadataKeyword {
  def toJson = "title" -> s.toJson
}
object Title extends (JsValue => Title) {
  def apply(json: JsValue) = json match {
    case JsString(s) => Title(s)
    case _           => deserializationError("string expected")
  }
}

case class Description(s: String) extends MetadataKeyword {
  def toJson = "description" -> s.toJson
}
object Description extends (JsValue => Description) {
  def apply(json: JsValue) = json match {
    case JsString(s) => Description(s)
    case _           => deserializationError("string expected")
  }
}

case class Default(v: JsValue) extends MetadataKeyword {
  def toJson = "default" -> v
}
