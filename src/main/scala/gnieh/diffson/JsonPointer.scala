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

import scala.annotation.tailrec

import spray.json._

/** A class to work with Json pointers according to http://tools.ietf.org/html/rfc6901.
 *  The behavior in case of invalid pointer is customizable by passing an error handler
 *  when instantiating.
 *
 *  @author Lucas Satabin
 */
class JsonPointer(errorHandler: PartialFunction[(JsValue, String, Pointer), JsValue]) {

  private def handle(value: JsValue, pointer: String, parent: Pointer): JsValue =
    errorHandler.orElse(allError)(value, pointer, parent)

  /** Parses a JSON pointer and returns the resolved path. */
  def parse(input: String): Pointer = {
    if (input == null || input.isEmpty)
      // shortcut if input is empty
      Pointer.empty
    else if (!input.startsWith("/")) {
      // a pointer MUST start with a '/'
      throw new PointerException("A JSON pointer must start with '/'")
    } else {
      // first gets the different parts of the pointer
      val parts = input.split("/")
        // the first element is always empty as the path starts with a '/'
        .drop(1)
      if (parts.size == 0) {
        // the pointer was simply "/"
        Path(Root, "")
      } else {
        // check that an occurrence of '~' is followed by '0' or '1'
        if (parts.exists(_.matches(".*~(?![01]).*"))) {
          throw new PointerException("Occurrences of '~' must be followed by '0' or '1'")
        } else {
          parts
            // transform the occurrences of '~1' into occurrences of '/'
            // transform the occurrences of '~0' into occurrences of '~'
            .map(_.replace("~1", "/").replace("~0", "~"))
            .foldLeft(Pointer.root)(_ / _)
        }
      }
    }
  }

  /** Evaluates the given path in the given JSON object.
   *  Upon missing elements in value, the error handler is called with the current value and element
   */
  @inline
  def evaluate(value: String, path: String): JsValue =
    evaluate(JsonParser(value), parse(path))

  /** Evaluates the given path in the given JSON object.
   *  Upon missing elements in value, the error handler is called with the current value and element
   */
  final def evaluate(value: JsValue, path: Pointer): JsValue =
    evaluate(value, path, Pointer.root)

  @tailrec
  private def evaluate(value: JsValue, path: Pointer, parent: Pointer): JsValue = (value, path) match {
    case (JsObject(obj), elem / tl) =>
      evaluate(obj.getOrElse(elem, JsNull), tl, parent / elem)
    case (JsArray(arr), (p @ IntIndex(idx)) / tl) =>
      if (idx >= arr.size)
        // we know (by construction) that the index is greater or equal to zero
        evaluate(handle(value, p, parent), tl, parent / p)
      else
        evaluate(arr(idx), tl, parent / p)
    case (arr: JsArray, "-" / tl) =>
      evaluate(handle(value, "-", parent), tl, parent / "-")
    case (_, Pointer.Empty) =>
      value
    case (_, elem / tl) =>
      evaluate(handle(value, elem, parent), tl, parent / elem)
  }

}

object IntIndex {
  private[this] val int = "(0|[1-9][0-9]*)".r
  def unapply(s: String): Option[Int] =
    s match {
      case int(i) => Some(i.toInt)
      case _      => None
    }
}
