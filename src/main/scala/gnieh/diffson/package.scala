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
package gnieh

import spray.json._

/** This package contains an implementation of Json JsonPatch, according to [RFC-6902](http://tools.ietf.org/html/rfc6902)
 */
package object diffson {

  type Pointer = List[String]

  type PointerErrorHandler = PartialFunction[(JsValue, String, Pointer), JsValue]

  implicit def s2path(s: String) = List(s)

  implicit def i2path(i: Int) = List(i.toString)

  private[diffson] val allError: PointerErrorHandler = {
    case (value, pointer, parent) =>
      throw new PointerException(s"element $pointer does not exist at path ${pointerString(parent)}")
  }

  implicit val pointer = new JsonPointer(allError)

  def pointerString(path: Pointer): String =
    path.map(_.replace("~", "~0").replace("/", "~1")).mkString("/", "/", "")

}
