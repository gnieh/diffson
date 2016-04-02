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

import spray.json._

sealed trait JsType {
  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError]
}

object JsType {

  def apply(json: JsValue): JsType = json match {
    case JsArray(_)                  => JsArrayType
    case JsBoolean(_)                => JsBooleanType
    case JsNumber(n) if n.isValidInt => JsIntegerType
    case JsNumber(_)                 => JsNumberType
    case JsNull                      => JsNullType
    case JsObject(_)                 => JsObjectType
    case JsString(_)                 => JsStringType
  }

}

case object JsArrayType extends JsType {
  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsArray(_) => Vector()
    case _          => Vector(ValidationError(pointer, f"Array expected but got value of type ${JsType(json)}"))
  }
}
case object JsBooleanType extends JsType {
  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsBoolean(_) => Vector()
    case _            => Vector(ValidationError(pointer, f"Boolean expected but got value of type ${JsType(json)}"))
  }
}
case object JsIntegerType extends JsType {
  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsNumber(n) if n.isValidInt => Vector()
    case _                           => Vector(ValidationError(pointer, f"Integer expected but got value of type ${JsType(json)}"))
  }
}
case object JsNumberType extends JsType {
  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsNumber(_) => Vector()
    case _           => Vector(ValidationError(pointer, f"Number expected but got value of type ${JsType(json)}"))
  }
}
case object JsNullType extends JsType {
  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsNull => Vector()
    case _      => Vector(ValidationError(pointer, f"Null expected but got value of type ${JsType(json)}"))
  }
}
case object JsObjectType extends JsType {
  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsObject(_) => Vector()
    case _           => Vector(ValidationError(pointer, f"Object expected but got value of type ${JsType(json)}"))
  }
}
case object JsStringType extends JsType {
  def validate(pointer: Pointer, json: JsValue): Vector[ValidationError] = json match {
    case JsString(_) => Vector()
    case _           => Vector(ValidationError(pointer, f"String expected but got value of type ${JsType(json)}"))
  }
}
