/*
 * Copyright 2022 Typelevel
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

import spray.json.JsValue

package object sprayJson {

  implicit object sprayJsonJsony extends Jsony[JsValue] {

    val Null: JsValue =
      spray.json.JsNull

    def makeArray(elems: Vector[JsValue]): JsValue =
      spray.json.JsArray(elems)

    def makeObject(fields: Map[String, JsValue]): JsValue =
      spray.json.JsObject(fields)

    def array(value: JsValue): Option[Vector[JsValue]] = value match {
      case spray.json.JsArray(elems) =>
        Some(elems)
      case _ =>
        None
    }

    def fields(value: JsValue): Option[Map[String, JsValue]] = value match {
      case spray.json.JsObject(fields) =>
        Some(fields)
      case _ =>
        None
    }

    def show(t: JsValue) = t.compactPrint

    def eqv(json1: JsValue, json2: JsValue) =
      json1 == json2

  }

}
