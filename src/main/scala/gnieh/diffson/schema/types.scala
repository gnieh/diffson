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

import spray.json._

sealed trait JsType {
  def validate(json: JsValue): Boolean
}

case object JsArrayType extends JsType {
  def validate(json: JsValue): Boolean = json match {
    case JsArray(_) => true
    case _          => false
  }
}
case object JsBooleanType extends JsType {
  def validate(json: JsValue): Boolean = json match {
    case JsBoolean(_) => true
    case _            => false
  }
}
case object JsIntegerType extends JsType {
  def validate(json: JsValue): Boolean = json match {
    case JsNumber(n) => n.isValidInt
    case _           => false
  }
}
case object JsNumberType extends JsType {
  def validate(json: JsValue): Boolean = json match {
    case JsNumber(_) => true
    case _           => false
  }
}
case object JsNullType extends JsType {
  def validate(json: JsValue): Boolean = json match {
    case JsNull => true
    case _      => false
  }
}
case object JsObjectType extends JsType {
  def validate(json: JsValue): Boolean = json match {
    case JsObject(_) => true
    case _           => false
  }
}
case object JsStringType extends JsType {
  def validate(json: JsValue): Boolean = json match {
    case JsString(_) => true
    case _           => false
  }
}
