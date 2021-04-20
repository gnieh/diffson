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
package jsonpatch

import jsonpointer._

import cats._
import cats.syntax.all._

import scala.annotation.tailrec
import scala.language.higherKinds

/** A patch operation to apply to a Json value */
sealed abstract class Operation[Json: Jsony] {
  val path: Pointer

  def apply[F[_]](value: Json)(implicit F: MonadError[F, Throwable]): F[Json] =
    action[F](value, path, Pointer.Root)

  // the action to perform in this operation. By default returns an object that is equal
  protected[this] def action[F[_]](value: Json, pointer: Pointer, parent: Pointer)(implicit F: MonadError[F, Throwable]): F[Json] = (value, pointer) match {
    case (_, Pointer.Root) =>
      F.pure(value)
    case (JsObject(fields), Inner(ObjectField(elem), tl)) if fields.contains(elem) =>
      action[F](fields(elem), tl, parent / elem)
        .map(fields.updated(elem, _))
        .map(JsObject(_))
    case (JsArray(elems), Inner(ArrayIndex(idx), tl)) =>
      if (idx >= elems.size) {
        F.raiseError(new PatchException(show"element $idx does not exist at path $parent"))
      } else {
        action[F](elems(idx), tl, parent / idx)
          .map { updated =>
            val (before, after) = elems.splitAt(idx)
            JsArray(before ++ (updated +: after.tail))
          }
      }
    case (_, Inner(elem, _)) =>
      F.raiseError(new PatchException(show"element ${elem.fold(identity[String], _.toString)} does not exist at path $parent"))
  }

}

/** Add (or replace if existing) the pointed element */
case class Add[Json: Jsony](path: Pointer, value: Json) extends Operation[Json] {

  override protected[this] def action[F[_]](original: Json, pointer: Pointer, parent: Pointer)(implicit F: MonadError[F, Throwable]): F[Json] =
    (original, pointer) match {
      case (_, Pointer.Root) =>
        // we are at the root value, simply return the replacement value
        F.pure(value)
      case (JsArray(arr), Leaf(Left("-"))) =>
        // insert the value at the end of the array
        F.pure(JsArray(arr :+ value))
      case (JsArray(arr), Leaf(ArrayIndex(idx))) =>
        if (idx > arr.size) {
          F.raiseError(new PatchException(show"element $idx does not exist at path $parent"))
        } else {
          // insert the value at the specified index
          val (before, after) = arr.splitAt(idx)
          F.pure(JsArray(before ++ (value +: after)))
        }
      case (JsObject(obj), Leaf(ObjectField(lbl))) =>
        // insert the new label
        F.pure(JsObject(obj.updated(lbl, value)))
      case _ =>
        super.action[F](original, pointer, parent)
    }

}

/** Remove the pointed element */
case class Remove[Json: Jsony](path: Pointer, old: Option[Json] = None) extends Operation[Json] {

  override protected[this] def action[F[_]](value: Json, pointer: Pointer, parent: Pointer)(implicit F: MonadError[F, Throwable]): F[Json] =
    (value, pointer) match {
      case (JsArray(arr), Leaf(ArrayIndex(idx))) =>
        if (idx >= arr.size) {
          // we know thanks to the extractor that the index cannot be negative
          F.raiseError(new PatchException(show"element $idx does not exist at path $parent"))
        } else {
          // remove the element at the given index
          val (before, after) = arr.splitAt(idx)
          F.pure(JsArray(before ++ after.tail))
        }
      case (JsArray(_), Leaf(Left("-"))) =>
        // how could we possibly remove an element that appears after the last one?
        F.raiseError(new PatchException(show"element - does not exist at path $parent"))
      case (JsObject(obj), Leaf(ObjectField(lbl))) if obj.contains(lbl) =>
        // remove the field from the object if present, otherwise, ignore it
        F.pure(JsObject(obj - lbl))
      case (_, Pointer.Root) =>
        F.raiseError(new PatchException("Cannot delete an empty path"))
      case _ =>
        super.action[F](value, pointer, parent)
    }

}

/** Replace the pointed element by the given value */
case class Replace[Json: Jsony](path: Pointer, value: Json, old: Option[Json] = None) extends Operation[Json] {

  override protected[this] def action[F[_]](original: Json, pointer: Pointer, parent: Pointer)(implicit F: MonadError[F, Throwable]): F[Json] =
    (original, pointer) match {
      case (_, Pointer.Root) =>
        // simply replace the root value by the replacement value
        F.pure(value)
      case (JsArray(arr), Leaf(Right(idx))) =>
        if (idx >= arr.size)
          F.raiseError(new PatchException(show"element $idx does not exist at path $parent"))
        else
          F.pure(JsArray(arr.updated(idx, value)))
      case (JsArray(_), Leaf(Left("-"))) =>
        F.raiseError(new PatchException(show"element - does not exist at path $parent"))
      case (JsObject(obj), Leaf(ObjectField(lbl))) =>
        if (obj.contains(lbl))
          F.pure(JsObject(obj.updated(lbl, value)))
        else
          F.raiseError(new PatchException(show"element $lbl does not exist at path $parent"))
      case _ =>
        super.action[F](original, pointer, parent)
    }

}

/** Move the pointed element to the new position */
case class Move[Json: Jsony](from: Pointer, path: Pointer) extends Operation[Json] {

  override def apply[F[_]](original: Json)(implicit F: MonadError[F, Throwable]): F[Json] = {
    @tailrec
    def prefix(p1: Pointer, p2: Pointer): Boolean = (p1, p2) match {
      case (Inner(h1, tl1), Inner(h2, tl2)) if h1 == h2 => prefix(tl1, tl2)
      case (Pointer.Root, Inner(_, _))                  => true
      case (_, _)                                       => false
    }
    if (prefix(from, path))
      F.raiseError(new PatchException("The destination path cannot be a descendant of the source path"))
    else
      for {
        value <- from.evaluate[F, Json](original)
        cleaned <- Remove[Json](from).apply[F](original)
        res <- Add[Json](path, value).apply[F](cleaned)
      } yield res
  }

}

/** Copy the pointed element to the new position */
case class Copy[Json: Jsony](from: Pointer, path: Pointer) extends Operation[Json] {

  override def apply[F[_]](original: Json)(implicit F: MonadError[F, Throwable]): F[Json] = for {
    value <- from.evaluate[F, Json](original)
    res <- Add[Json](path, value).apply[F](original)
  } yield res

}

/** Test that the pointed element is equal to the given value */
case class Test[Json: Jsony](path: Pointer, value: Json) extends Operation[Json] {

  override def apply[F[_]](original: Json)(implicit F: MonadError[F, Throwable]): F[Json] =
    path.evaluate[F, Json](original).flatMap { orig =>
      if (value != orig)
        F.raiseError(new PatchException(show"test failed at path $path"))
      else
        F.pure(original)
    }

}

case class JsonPatch[Json: Jsony](ops: List[Operation[Json]]) {
  def apply[F[_]](json: Json)(implicit F: MonadError[F, Throwable]) =
    ops.foldM(json)((json, op) => op[F](json))
}

object JsonPatch {

  implicit def JsonPatchPatch[F[_], Json](implicit F: MonadError[F, Throwable], Json: Jsony[Json]): Patch[F, Json, JsonPatch[Json]] =
    new Patch[F, Json, JsonPatch[Json]] {
      def apply(json: Json, patch: JsonPatch[Json]): F[Json] =
        patch[F](json)
    }

  def apply[Json: Jsony](ops: Operation[Json]*): JsonPatch[Json] =
    JsonPatch(ops.toList)

}
