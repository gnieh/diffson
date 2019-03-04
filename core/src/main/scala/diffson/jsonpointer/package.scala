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
package diffson

import cats._
import cats.implicits._
import cats.data.Chain

import scala.language.higherKinds

package object jsonpointer {

  type Part = Either[String, Int]
  type Pointer = Chain[Part]

  object Pointer {

    val Root: Pointer = Chain.empty

    private val IsDigit = "(0|[1-9][0-9]*)".r

    def apply(elems: String*): Pointer = Chain.fromSeq(elems.map {
      case IsDigit(idx) => Right(idx.toInt)
      case key          => Left(key)
    })

    def unapply(p: Pointer): Option[(Part, Pointer)] =
      p.uncons

    def parse[F[_]](input: String)(implicit F: MonadError[F, Throwable]): F[Pointer] =
      if (input == null || input.isEmpty) {
        // shortcut if input is empty
        F.pure(Pointer.Root)
      } else if (!input.startsWith("/")) {
        // a pointer MUST start with a '/'
        F.raiseError(new PointerException("A JSON pointer must start with '/'"))
      } else {
        // first gets the different parts of the pointer
        val parts = input.split("/")
          // the first element is always empty as the path starts with a '/'
          .drop(1)
        if (parts.length == 0) {
          // the pointer was simply "/"
          F.pure(Pointer(""))
        } else {
          // check that an occurrence of '~' is followed by '0' or '1'
          if (parts.exists(_.matches(".*~(?![01]).*"))) {
            F.raiseError(new PointerException("Occurrences of '~' must be followed by '0' or '1'"))
          } else {
            val allParts = if (input.endsWith("/")) parts :+ "" else parts

            val elems = allParts
              // transform the occurrences of '~1' into occurrences of '/'
              // transform the occurrences of '~0' into occurrences of '~'
              .map(_.replace("~1", "/").replace("~0", "~"))
            F.pure(Pointer(elems: _*))
          }
        }
      }

  }

  object Leaf {

    def unapply(p: Pointer): Option[Part] =
      p.uncons.flatMap {
        case (a, Chain.nil) => Some(a)
        case _              => None
      }

  }

  object ArrayIndex {
    def unapply(e: Part): Option[Int] = e.toOption
  }

  object ObjectField {
    def unapply(e: Part): Option[String] = Some(e.fold(identity, _.toString))
  }

  implicit class PointerOps(val p: Pointer) extends AnyVal {

    def /(s: String): Pointer =
      p.append(Left(s))

    def /(i: Int): Pointer =
      p.append(Right(i))

    def evaluate[F[_], Json](json: Json)(implicit F: MonadError[F, Throwable], Json: Jsony[Json]): F[Json] =
      F.tailRecM((json, p, Pointer.Root)) {
        case (JsObject(obj), Pointer(Left(elem), tl), parent) =>
          F.pure(Left((obj.getOrElse(elem, Json.Null), tl, parent.append(Left(elem)))))
        case (JsArray(arr), Pointer(Right(idx), tl), parent) =>
          if (idx >= arr.size)
            // we know (by construction) that the index is greater or equal to zero
            F.raiseError(new PatchException(show"element $idx does not exist at path $parent"))
          else
            F.pure(Left(arr(idx), tl, parent.append(Right(idx))))
        case (value, Pointer.Root, _) =>
          F.pure(Right(value))
        case (_, Pointer(Left(elem), tl), parent) =>
          F.raiseError(new PatchException(show"element $elem does not exist at path $parent"))
      }

  }

}
