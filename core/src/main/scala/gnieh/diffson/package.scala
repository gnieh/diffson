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

import scala.collection.immutable.Queue

/** This package contains an implementation of Json JsonPatch, according to [RFC-6902](http://tools.ietf.org/html/rfc6902)
 */
package object diffson {

  private val IsDigit = "(0|[1-9][0-9]*)".r

  type Part = Either[String, Int]
  type Pointer = Queue[Part]

  implicit class PointerOps(val pointer: Pointer) extends AnyVal {
    def /(key: String): Pointer = pointer :+ Left(key)

    def /(idx: Int): Pointer = pointer :+ Right(idx)

    def serialize: String = if (pointer.isEmpty) "" else "/" + pointer.map {
      case Left(l)  => l.replace("~", "~0").replace("/", "~1")
      case Right(r) => r.toString
    }.mkString("/")
  }

  object Pointer {
    val Root: Pointer = Queue.empty

    def apply(elems: String*): Pointer = Queue(elems.map {
      case IsDigit(idx) => Right(idx.toInt)
      case key          => Left(key)
    }: _*)

    def unapplySeq(pointer: Pointer): Option[Queue[Part]] = Queue.unapplySeq(pointer)

    /** Parses a JSON pointer and returns the resolved path. */
    def parse(input: String): Pointer = {
      if (input == null || input.isEmpty)
        // shortcut if input is empty
        Pointer.Root
      else if (!input.startsWith("/")) {
        // a pointer MUST start with a '/'
        throw new PointerException("A JSON pointer must start with '/'")
      } else {
        // first gets the different parts of the pointer
        val parts = input.split("/")
          // the first element is always empty as the path starts with a '/'
          .drop(1)
        if (parts.length == 0) {
          // the pointer was simply "/"
          Pointer("")
        } else {
          // check that an occurrence of '~' is followed by '0' or '1'
          if (parts.exists(_.matches(".*~(?![01]).*"))) {
            throw new PointerException("Occurrences of '~' must be followed by '0' or '1'")
          } else {
            val elems = parts
              // transform the occurrences of '~1' into occurrences of '/'
              // transform the occurrences of '~0' into occurrences of '~'
              .map(_.replace("~1", "/").replace("~0", "~"))
            Pointer(elems: _*)
          }
        }
      }
    }
  }

  protected[diffson] object ArrayIndex {
    def unapply(e: Part): Option[Int] = e.toOption
  }

  protected[diffson] object ObjectField {
    def unapply(e: Part): Option[String] = Some(e.fold(identity, _.toString))
  }

}
