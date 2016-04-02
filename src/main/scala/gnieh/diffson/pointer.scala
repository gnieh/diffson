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

/** A parsed Json pointer as per RFC-6901.
 *
 *  @author Lucas Satabin
 */
sealed abstract class Pointer {

  def /(elem: String): Pointer =
    Path(this, elem)

  def /(elem: Int): Pointer =
    Path(this, elem.toString)

}

object Pointer {

  val root: Pointer =
    Root

  val empty: Pointer =
    Root

  val Empty: Pointer =
    Root

  def apply(elems: String*) =
    elems.foldLeft(root)(_ / _)

  def unapplySeq(pointer: Pointer): Option[Seq[String]] = pointer match {
    case Root             => Some(Seq())
    case Path(base, elem) => unapplySeq(base).map(_ :+ elem)
  }

}

object / {
  def unapply(pointer: Pointer): Option[(String, Pointer)] = pointer match {
    case Root             => None
    case Path(Root, elem) => Some(elem -> Pointer.empty)
    case Path(base, elem) => unapply(base).fold(Some(elem -> Pointer.empty)) {
      case (first, rest) => Some(first -> Path(rest, elem))
    }
  }
}

private case object Root extends Pointer {
  override def toString =
    "/"
}

private final case class Path(prefix: Pointer, elem: String) extends Pointer {
  override def toString =
    prefix match {
      case Root => f"/${elem.replace("~", "~0").replace("/", "~1")}"
      case _    => f"${prefix}${elem.replace("~", "~0").replace("/", "~1")}"
    }
}
