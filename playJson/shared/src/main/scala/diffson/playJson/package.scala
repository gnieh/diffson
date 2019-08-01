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

import play.api.libs.json._

package object playJson {

  implicit object playJsonJsony extends Jsony[JsValue] {

    val Null: JsValue =
      play.api.libs.json.JsNull

    def makeArray(elems: Vector[JsValue]): JsValue =
      play.api.libs.json.JsArray(elems)

    def makeObject(fields: Map[String, JsValue]): JsValue =
      play.api.libs.json.JsObject(fields)

    def array(value: JsValue): Option[Vector[JsValue]] = value match {
      case play.api.libs.json.JsArray(elems) =>
        Some(elems.toVector)
      case _ =>
        None
    }

    def fields(value: JsValue): Option[Map[String, JsValue]] = value match {
      case play.api.libs.json.JsObject(fields) =>
        Some(fields.toMap)
      case _ =>
        None
    }

    def show(json: JsValue): String =
      Json.stringify(json)

    def eqv(json1: JsValue, json2: JsValue): Boolean =
      json1 == json2

  }
}
