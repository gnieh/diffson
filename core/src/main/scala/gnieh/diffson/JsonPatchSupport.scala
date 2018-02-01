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
package diffson

import scala.annotation.tailrec

import scala.collection.immutable.VectorBuilder

trait JsonPatchSupport[JsValue] {
  this: DiffsonInstance[JsValue] =>

  import provider._

  class WithFilter(p: Operation => Boolean, patch: JsonPatch) {

    def map(f: Operation => Operation): JsonPatch =
      patch.flatMap(op => if (p(op)) JsonPatch(f(op)) else JsonPatch())

    def flatMap(f: Operation => JsonPatch): JsonPatch =
      patch.flatMap(op => if (p(op)) f(op) else JsonPatch())

    def withFilter(p2: Operation => Boolean): WithFilter =
      new WithFilter(op => p(op) && p2(op), patch)

    def foreach(f: Operation => Unit): Unit =
      patch.foreach(op => if (p(op)) f(op))

  }

  /** JsonPatch companion object allowing to create `JsonPatch` objects from strings or operations.
   *
   *  @author Lucas Satabin
   */
  object JsonPatch {

    def apply(ops: Operation*): JsonPatch =
      new JsonPatch(ops.toList)

    /** Parses a Json patch as per http://tools.ietf.org/html/rfc6902 */
    def parse(patch: String): JsonPatch =
      unmarshall[JsonPatch](parseJson(patch))

    def apply(json: JsValue): JsonPatch =
      unmarshall[JsonPatch](json)

  }

  /** A Json patch object according to http://tools.ietf.org/html/rfc6902
   *
   *  @author Lucas Satabin
   */
  case class JsonPatch(ops: List[Operation]) {

    /** Applies this patch to the given Json valued and returns the patched value */
    def apply(json: String): String =
      compactPrint(apply(parseJson(json)))

    /** Applies this patch to the given Json value, and returns the patched value */
    def apply(value: JsValue): JsValue =
      ops.foldLeft(value) { (acc, op) =>
        op(acc)
      }

    /** Applies this patch to the given Json value, and returns the patched value.
     *  It assumes that the shape of the patched object is the same as the input one.
     *  If it is not the case, an exception will be raised
     */
    def apply[T: Marshaller: Unmarshaller](value: T): T =
      unmarshall[T](apply(marshall(value)))

    /** Create a patch that applies `this` patch and then `that` patch */
    def andThen(that: JsonPatch): JsonPatch =
      new JsonPatch(this.ops ++ that.ops)

    def map(f: Operation => Operation): JsonPatch =
      JsonPatch(ops.map(f))

    def flatMap(f: Operation => JsonPatch): JsonPatch =
      JsonPatch(for {
        op <- ops
        JsonPatch(ops) = f(op)
        op <- ops
      } yield op)

    def filter(p: Operation => Boolean): JsonPatch =
      JsonPatch(ops.filter(p))

    def withFilter(p: Operation => Boolean): WithFilter =
      new WithFilter(p, this)

    def foldLeft[Res](zero: Res)(f: (Res, Operation) => Res): Res =
      ops.foldLeft(zero)(f)

    def foldRight[Res](zero: Res)(f: (Operation, Res) => Res): Res =
      ops.foldRight(zero)(f)

    def foreach(f: Operation => Unit): Unit =
      ops.foreach(f)

    def collect[T](pf: PartialFunction[Operation, T]): Seq[T] =
      ops.collect(pf)

    override def toString: String = prettyPrint(marshall(this))

  }

  /** A patch operation to apply to a Json value */
  sealed abstract class Operation {
    val path: JsonPointer

    /** Applies this operation to the given Json value */
    def apply(json: String): String =
      compactPrint(apply(parseJson(json)))

    /** Applies this operation to the given Json value */
    def apply(value: JsValue): JsValue =
      action(value, path, Pointer.Root)

    // internals

    // the action to perform in this operation. By default returns an object that is equal
    protected[this] def action(value: JsValue, pointer: JsonPointer, parent: JsonPointer): JsValue = (value, pointer.path) match {
      case (_, Pointer.Root) =>
        value
      case (JsObject(fields), ObjectField(elem) +: tl) if fields.contains(elem) =>
        val fields1 = fields.map {
          case (name, value) if name == elem =>
            (name, action(value, tl, parent / elem))
          case f =>
            f
        }
        JsObject(fields1)
      case (JsArray(elems), ArrayIndex(idx) +: tl) =>
        if (idx >= elems.size)
          throw new PatchException(f"element $idx does not exist at path ${parent.serialize}")
        val (before, after) = elems.splitAt(idx)
        val builder = new VectorBuilder[JsValue]
        builder.sizeHint(elems.size)
        builder ++= before
        builder += action(elems(idx), tl, Right(idx) +: parent)
        builder ++= after.view(1, after.size)
        JsArray(builder.result)
      case (_, elem +: _) =>
        throw new PatchException(s"element ${elem.fold(identity, _.toString)} does not exist at path ${parent.serialize}")
    }

    protected object ArrayIndex {
      def unapply(e: Part): Option[Int] = e.toOption
    }

    protected object ObjectField {
      def unapply(e: Part): Option[String] = Some(e.fold(identity, _.toString))
    }

  }

  /** Add (or replace if existing) the pointed element */
  case class Add(path: JsonPointer, value: JsValue) extends Operation {

    override def action(original: JsValue, pointer: JsonPointer, parent: JsonPointer): JsValue = (original, pointer.path) match {
      case (_, Pointer.Root) =>
        // we are at the root value, simply return the replacement value
        value
      case (JsArray(arr), Pointer(Left("-"))) =>
        // insert the value at the end of the array
        JsArray(arr :+ value)
      case (JsArray(arr), Pointer(ArrayIndex(idx))) =>
        if (idx > arr.size) {
          throw new PatchException(f"element $idx does not exist at path ${parent.serialize}")
        } else {
          // insert the value at the specified index
          val (before, after) = arr.splitAt(idx)
          val builder = new VectorBuilder[JsValue]
          builder.sizeHint(arr.size + 1)
          builder ++= before
          builder += value
          builder ++= after
          JsArray(builder.result)
        }
      case (JsObject(obj), Pointer(ObjectField(lbl))) =>
        // insert the new label
        JsObject(obj.updated(lbl, value))
      case _ =>
        super.action(original, pointer, parent)
    }
  }

  /** Remove the pointed element */
  case class Remove(path: JsonPointer, old: Option[JsValue] = None) extends Operation {

    override def action(original: JsValue, pointer: JsonPointer, parent: JsonPointer): JsValue =
      (original, pointer.path) match {
        case (JsArray(arr), Pointer(ArrayIndex(idx))) =>
          if (idx >= arr.size) {
            // we know thanks to the extractor that the index cannot be negative
            throw new PatchException(f"element $idx does not exist at path ${parent.serialize}")
          } else {
            // remove the element at the given index
            val (before, after) = arr.splitAt(idx)
            JsArray(before ++ after.tail)
          }
        case (JsArray(_), Pointer(Left("-"))) =>
          // how could we possibly remove an element that appears after the last one?
          throw new PatchException(f"element - does not exist at path ${parent.serialize}")
        case (JsObject(obj), Pointer(ObjectField(lbl))) if obj.contains(lbl) =>
          // remove the field from the object if present, otherwise, ignore it
          JsObject(obj - lbl)
        case (_, Pointer.Root) =>
          throw new PatchException("Cannot delete an empty path")
        case _ =>
          super.action(original, pointer, parent)
      }
  }

  /** Replace the pointed element by the given value */
  case class Replace(path: JsonPointer, value: JsValue, old: Option[JsValue] = None) extends Operation {

    override def action(original: JsValue, pointer: JsonPointer, parent: JsonPointer): JsValue =
      (original, pointer.path) match {
        case (_, Pointer.Root) =>
          // simply replace the root value by the replacement value
          value
        case (JsArray(arr), Pointer(Right(idx))) =>
          if (idx >= arr.size) {
            throw new PatchException(f"element $idx does not exist at path ${parent.serialize}")
          } else {
            val (before, after) = arr.splitAt(idx)
            val builder = new VectorBuilder[JsValue]
            builder.sizeHint(arr.size)
            builder ++= before
            builder += value
            builder ++= after.view(1, after.size)
            JsArray(builder.result)
          }
        case (JsArray(_), Pointer(Left("-"))) =>
          throw new PatchException(f"element - does not exist at path ${parent.serialize}")
        case (JsObject(obj), Pointer(ObjectField(lbl))) =>
          if (obj.contains(lbl)) {
            JsObject(obj.updated(lbl, value))
          } else {
            throw new PatchException(s"element $lbl does not exist at path ${parent.serialize}")
          }
        case _ =>
          super.action(original, pointer, parent)
      }

  }

  /** Move the pointed element to the new position */
  case class Move(from: JsonPointer, path: JsonPointer) extends Operation {

    override def apply(original: JsValue): JsValue = {
      @tailrec
      def prefix(p1: JsonPointer, p2: JsonPointer): Boolean = (p1.path, p2.path) match {
        case (h1 +: tl1, h2 +: tl2) if h1 == h2 => prefix(tl1, tl2)
        case (Pointer.Root, _ +: _)             => true
        case (_, _)                             => false
      }
      if (prefix(from, path))
        throw new PatchException("The destination path cannot be a descendant of the source path")

      val remove = Remove(from)
      val cleaned = remove(original)
      val add = Add(path, from.evaluate(original))
      add(cleaned)
    }

  }

  /** Copy the pointed element to the new position */
  case class Copy(from: JsonPointer, path: JsonPointer) extends Operation {

    override def apply(original: JsValue): JsValue = {
      val add = Add(path, from.evaluate(original))
      add(original)
    }
  }

  /** Test that the pointed element is equal to the given value */
  case class Test(path: JsonPointer, value: JsValue) extends Operation {

    override def apply(original: JsValue): JsValue = {
      val orig = path.evaluate(original)
      if (value != orig)
        throw new PatchException(s"test failed at path ${path.serialize}")
      else
        original
    }
  }

}
