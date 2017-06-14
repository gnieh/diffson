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

import scala.annotation.tailrec

trait JsonPointerSupport[JsValue] {
  this: DiffsonInstance[JsValue] =>

  import provider._

  implicit val pointer = new JsonPointer()

  /** A class to work with Json pointers according to http://tools.ietf.org/html/rfc6901.
   *  The behavior in case of invalid pointer is customizable by passing an error handler
   *  when instantiating.
   *
   *  @author Lucas Satabin
   */
  class JsonPointer {

    /** Evaluates the given path in the given JSON object.
     *  Upon missing elements in value, the error handler is called with the current value and element
     */
    def evaluate(value: String, path: String): JsValue =
      evaluate(parseJson(value), Pointer.parse(path), Pointer.Root)

    /** Evaluates the given path in the given JSON object.
     *  Upon missing elements in value, the error handler is called with the current value and element
     */
    final def evaluate(value: JsValue, path: Pointer): JsValue =
      evaluate(value, path, Pointer.Root)

    @tailrec
    private def evaluate(value: JsValue, path: Pointer, parent: Pointer): JsValue = (value, path) match {
      case (JsObject(obj), Left(elem) +: tl) =>
        evaluate(obj.getOrElse(elem, JsNull), tl, parent :+ Left(elem))
      case (JsArray(arr), Right(idx) +: tl) =>
        if (idx >= arr.size)
          // we know (by construction) that the index is greater or equal to zero
          evaluate(errorHandler(value, idx.toString, parent), tl, parent :+ Right(idx))
        else
          evaluate(arr(idx), tl, parent :+ Right(idx))
      case (arr @ JsArray(_), Left("-") +: tl) =>
        evaluate(errorHandler(value, "-", parent), tl, parent / "-")
      case (_, Pointer.Root) =>
        value
      case (_, Left(elem) +: tl) =>
        evaluate(errorHandler(value, elem, parent), tl, parent / elem)
    }

  }

}
