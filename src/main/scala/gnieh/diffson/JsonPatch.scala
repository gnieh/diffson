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

import spray.json._

import scala.annotation.tailrec

/** Thrown whenever a problem is encountered when applying a patch */
class PatchException(msg: String) extends Exception(msg)

/** A Json patch object according to http://tools.ietf.org/html/rfc6902
 *
 *  @author Lucas Satabin
 */
final case class JsonPatch(ops: List[Operation]) {
  lazy val toJson: JsArray =
    JsArray(ops.map(_.toJson).toVector)

  /** Applies this patch to the given Json valued and returns the patched value */
  def apply(json: String, compacted: Boolean = false): String = {
    val patched = apply(JsonParser(json))
    if (compacted)
      patched.compactPrint
    else
      patched.prettyPrint
  }

  /** Applies this patch to the given Json value, and returns the patched value */
  def apply(value: JsValue): JsValue =
    ops.foldLeft(value) { (acc, op) =>
      op(acc)
    }

  /** Applies this patch to the given Json value, and returns the patched value.
   *  It assumes that the shape of the patched object is the same as the input one.
   *  If it is not the case, an exception will be raised
   */
  def apply[T: JsonFormat](value: T): T =
    apply(value.toJson).convertTo[T]

  /** Create a patch that applies `this` patch and then `that` patch */
  def andThen(that: JsonPatch): JsonPatch =
    JsonPatch(this.ops ++ that.ops)

  override def toString = toJson.prettyPrint

}

/** JsonPatch companion object allowing to create `JsonPatch` objects from strings or operations.
 *
 *  @author Lucas Satabin
 */
object JsonPatch {

  import DiffsonProtocol._

  def apply(ops: Operation*): JsonPatch =
    new JsonPatch(ops.toList)

  /** Parses a Json patch as per http://tools.ietf.org/html/rfc6902 */
  def parse(patch: String): JsonPatch =
    JsonParser(patch).convertTo[JsonPatch]

}

/** A patch operation to apply to a Json value */
sealed abstract class Operation {
  val path: Pointer
  def toJson: JsObject

  /** Applies this operation to the given Json value */
  def apply(json: String, compacted: Boolean = false): String = {
    val patched = apply(JsonParser(json))
    if (compacted)
      patched.compactPrint
    else
      patched.prettyPrint
  }

  /** Applies this operation to the given Json value */
  def apply(value: JsValue): JsValue = action(value, path)

  // internals

  // the action to perform in this operation. By default returns an object that is equal
  protected[this] def action(value: JsValue, pointer: Pointer): JsValue = (value, pointer) match {
    case (_, Nil) =>
      value
    case (JsObject(fields), elem :: tl) if fields.contains(elem) =>
      val fields1 = fields map {
        case (name, value) if name == elem =>
          (name, action(value, tl))
        case f =>
          f
      }
      JsObject(fields1)
    case (JsArray(elems), IntIndex(idx) :: tl) =>
      if (idx > elems.size)
        throw new PatchException("index out of bounds " + idx)
      val elems1 = elems.take(idx) ++ Vector(action(elems(idx), tl)) ++ elems.drop(idx + 1)
      JsArray(elems1)
    case (_, elem :: _) =>
      throw new PatchException(s"element $elem does not exist in ${value.prettyPrint}")
  }

}

/** Add (or replace if existing) the pointed element */
final case class Add(path: Pointer, value: JsValue) extends Operation {

  lazy val toJson =
    JsObject(
      "op" -> JsString("add"),
      "path" -> JsString(pointerString(path)),
      "value" -> value)

  override def action(original: JsValue, pointer: Pointer): JsValue = (original, pointer) match {
    case (_, Nil) =>
      // we are at the root value, simply return the replacement value
      value
    case (JsArray(arr), List("-")) =>
      // insert the value at the end of the array
      JsArray(arr ++ Vector(value))
    case (JsArray(arr), List(IntIndex(idx))) =>
      if (idx > arr.size)
        throw new PatchException(s"Index out of bounds $idx")
      else
        // insert the value at the specified index
        JsArray(arr.take(idx) ++ Vector(value) ++ arr.drop(idx))
    case (JsObject(obj), List(lbl)) =>
      // remove the label if it is present
      val cleaned = obj filter (_._1 != lbl)
      // insert the new label
      JsObject(cleaned.updated(lbl, value))
    case _ =>
      super.action(original, pointer)
  }
}

/** Remove the pointed element */
final case class Remove(path: Pointer) extends Operation {

  lazy val toJson =
    JsObject(
      "op" -> JsString("remove"),
      "path" -> JsString(pointerString(path)))

  override def action(original: JsValue, pointer: Pointer): JsValue =
    (original, pointer) match {
      case (JsArray(arr), List(IntIndex(idx))) =>
        if (idx >= arr.size)
          // we know thanks to the extractor that the index cannot be negative
          throw new PatchException(s"Index out of bounds $idx")
        else
          // remove the element at the given index
          JsArray(arr.take(idx) ++ arr.drop(idx + 1))
      case (JsArray(_), List("-")) =>
        // how could we possibly remove an element that appears after the last one?
        throw new PatchException("Index out of bounds -")
      case (JsObject(obj), List(lbl)) if obj.contains(lbl) =>
        // remove the field from the object if present, otherwise, ignore it
        JsObject(obj filter (_._1 != lbl))
      case (_, Nil) =>
        throw new PatchException("Cannot delete an empty path")
      case _ =>
        super.action(original, pointer)
    }
}

/** Replace the pointed element by the given value */
final case class Replace(path: Pointer, value: JsValue) extends Operation {

  lazy val toJson =
    JsObject(
      "op" -> JsString("replace"),
      "path" -> JsString(pointerString(path)),
      "value" -> value)

  override def action(original: JsValue, pointer: Pointer): JsValue =
    (original, pointer) match {
      case (_, Nil) =>
        // simply replace the root value by the replacement value
        value
      case (JsArray(arr), List(IntIndex(idx))) =>
        if (idx >= arr.size)
          throw new PatchException(s"Index out of bounds $idx")
        else
          JsArray(arr.take(idx) ++ Vector(value) ++ arr.drop(idx + 1))
      case (JsArray(_), List("-")) =>
        throw new PatchException("Index out of bounds -")
      case (JsObject(obj), List(lbl)) if obj.contains(lbl) =>
        val cleaned = obj filter (_._1 != lbl)
        JsObject(cleaned.updated(lbl, value))
      case (JsObject(_), List(lbl)) =>
        throw new PatchException(s"element $lbl does not exist in ${original.prettyPrint}")
      case _ =>
        super.action(original, pointer)
    }

}

/** Move the pointed element to the new position */
final case class Move(from: Pointer, path: Pointer) extends Operation {

  lazy val toJson =
    JsObject(
      "op" -> JsString("move"),
      "from" -> JsString(pointerString(from)),
      "path" -> JsString(pointerString(path)))

  override def apply(original: JsValue): JsValue = {
    @tailrec
    def prefix(p1: Pointer, p2: Pointer): Boolean = (p1, p2) match {
      case (h1 :: tl1, h2 :: tl2) if h1 == h2 => prefix(tl1, tl2)
      case (Nil, _ :: _)                      => true
      case (_, _)                             => false
    }
    if (prefix(from, path))
      throw new PatchException("The path were to move cannot be a descendent of the from path")

    val cleaned = Remove(from)(original)
    Add(path, pointer.evaluate(original, from))(cleaned)
  }

}

/** Copy the pointed element to the new position */
final case class Copy(from: Pointer, path: Pointer) extends Operation {

  lazy val toJson =
    JsObject(
      "op" -> JsString("copy"),
      "from" -> JsString(pointerString(from)),
      "path" -> JsString(pointerString(path)))

  override def apply(original: JsValue): JsValue =
    Add(path, pointer.evaluate(original, from))(original)
}

/** Test that the pointed element is equal to the given value */
final case class Test(path: Pointer, value: JsValue) extends Operation {

  lazy val toJson =
    JsObject(
      "op" -> JsString("test"),
      "path" -> JsString(pointerString(path)),
      "value" -> value)

  override def apply(original: JsValue): JsValue = {
    val orig = JsonPointer.evaluate(original, path)
    if (value != orig)
      throw new PatchException(s"${orig.prettyPrint} is not equal to ${value.prettyPrint}")
    else
      original
  }
}
