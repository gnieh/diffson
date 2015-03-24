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

import spray.json.JsString

sealed trait JsType {
  def toJson: JsString
}

case object JsArrayType extends JsType {
  def toJson = JsString("array")
}
case object JsBooleanType extends JsType {
  def toJson = JsString("boolean")
}
case object JsIntegerType extends JsType {
  def toJson = JsString("integer")
}
case object JsNumberType extends JsType {
  def toJson = JsString("number")
}
case object JsNullType extends JsType {
  def toJson = JsString("null")
}
case object JsObjectType extends JsType {
  def toJson = JsString("object")
}
case object JsStringType extends JsType {
  def toJson = JsString("string")
}
