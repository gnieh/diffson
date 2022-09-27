/*
 * Copyright 2022 Lucas Satabin
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

import cats._
import cats.syntax.all._
import cats.data.Chain

import scala.util.Try

import scala.collection.compat._
import scala.collection.compat.immutable.ArraySeq

package object jsonpointer {

  type Part = Either[String, Int]
  case class Pointer private[jsonpointer] (parts: Chain[Part]) extends AnyVal {

    def /(s: String): Pointer =
      Pointer(parts.append(Left(s)))

    def /(i: Int): Pointer =
      Pointer(parts.append(Right(i)))

    def evaluate[F[_], Json](json: Json)(implicit F: MonadError[F, Throwable], Json: Jsony[Json]): F[Json] =
      F.tailRecM((json, Pointer(parts), Pointer.Root)) {
        case (JsObject(obj), Inner(Left(elem), tl), parent) =>
          F.pure(Left((obj.getOrElse(elem, Json.Null), tl, parent / elem)))
        case (JsArray(arr), Inner(Right(idx), tl), parent) =>
          if (idx >= arr.size)
            // we know (by construction) that the index is greater or equal to zero
            F.raiseError(new PointerException(show"element $idx does not exist at path $parent"))
          else
            F.pure(Left((arr(idx), tl, parent / idx)))
        case (value, Pointer.Root, _) =>
          F.pure(Right(value))
        case (_, Inner(elem, _), parent) =>
          val elems = elem.fold(identity, _.toString)
          F.raiseError(new PointerException(show"element $elems does not exist at path $parent"))
      }

  }

  object Pointer {

    val Root: Pointer = new Pointer(Chain.empty)

    private val IsNumber = "(0|[1-9][0-9]*)".r

    def apply(elems: String*): Pointer = new Pointer(Chain.fromSeq(elems.map {
      case s @ IsNumber(idx) => Try(idx.toInt).liftTo[Either[Throwable, *]].leftMap(_ => s)
      case key               => Left(key)
    }))

    def unapply(p: Pointer): Some[Chain[Part]] =
      Some(p.parts)

    def parse[F[_]](input: String)(implicit F: MonadError[F, Throwable]): F[Pointer] =
      if (input == null || input.isEmpty) {
        // shortcut if input is empty
        F.pure(Pointer.Root)
      } else if (!input.startsWith("/")) {
        // a pointer MUST start with a '/'
        F.raiseError(new PointerException("A JSON pointer must start with '/'"))
      } else {
        // first gets the different parts of the pointer
        val parts = input
          .split("/")
          // the first element is always empty as the path starts with a '/'
          .drop(1)
        if (parts.length == 0) {
          // the pointer was simply "/"
          F.pure(Pointer(""))
        } else {
          // check that an occurrence of '~' is followed by '0' or '1'
          if (parts.exists(_.matches(".*~(?:[^01]|$).*"))) {
            F.raiseError(new PointerException("Occurrences of '~' must be followed by '0' or '1'"))
          } else {
            val allParts = if (input.endsWith("/")) parts :+ "" else parts

            val elems = allParts
              // transform the occurrences of '~1' into occurrences of '/'
              // transform the occurrences of '~0' into occurrences of '~'
              .map(_.replace("~1", "/").replace("~0", "~"))
            F.pure(Pointer(ArraySeq.unsafeWrapArray(elems): _*))
          }
        }
      }

    implicit val show: Show[Pointer] = Show.show[Pointer](pointer =>
      if (pointer.parts.isEmpty)
        ""
      else
        "/" + pointer.parts
          .map {
            case Left(l)  => l.replace("~", "~0").replace("/", "~1")
            case Right(r) => r.toString
          }
          .toList
          .mkString("/"))

  }

  object Inner {

    def unapply(parts: Pointer): Option[(Part, Pointer)] =
      parts.parts.uncons.map { case (h, t) => (h, new Pointer(t)) }

  }

  object Leaf {

    def unapply(p: Pointer): Option[Part] =
      p.parts.uncons.flatMap {
        case (a, rest) if rest.isEmpty => Some(a)
        case _                         => None
      }

  }

  object ArrayIndex {
    def unapply(e: Part): Option[Int] = e.toOption
  }

  object ObjectField {
    def unapply(e: Part): Option[String] = Some(e.fold(identity, _.toString))
  }

}
