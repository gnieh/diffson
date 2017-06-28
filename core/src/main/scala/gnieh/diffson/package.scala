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

  type Part = Either[String, Int]
  type Pointer = Queue[Part]

  object Pointer {
    val Root: Pointer = Queue.empty

    private val IsDigit = "(0|[1-9][0-9]*)".r

    def apply(elems: String*): Pointer = Queue(elems.map {
      case IsDigit(idx) => Right(idx.toInt)
      case key          => Left(key)
    }: _*)

    def unapplySeq(pointer: Pointer): Option[Queue[Part]] = Queue.unapplySeq(pointer)

  }

}
