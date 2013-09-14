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

import net.liftweb.json._
import JsonDSL._

import scala.annotation.tailrec

/** Thrown whenever a problem is encountered when applying a patch */
class PatchException(msg: String) extends Exception(msg)

/** A Json patch object according to http://tools.ietf.org/html/rfc6902
 *
 *  @author Lucas Satabin
 */
final case class JsonPatch(ops: List[Operation]) {
  lazy val toJson: JArray =
    JArray(ops.map(_.toJson))

  /** Applies this patch to the given Json valued and returns the patched value */
  def apply(json: String, compacted: Boolean = false): String = {
    val patched = apply(JsonParser.parse(json))
    if(compacted)
      compact(render(patched))
    else
      pretty(render(patched))
  }

  /** Applies this patch to the given Json value, and returns the patched value */
  def apply(value: JValue): JValue =
    ops.foldLeft(value) { (acc, op) =>
      op(acc)
    }

  /** Applies this patch to the given Json value, and returns the patched value.
   *  It assumes that the shape of the patched object is the same as the input one.
   *  If it is not the case, an exception will be raised */
  def apply[T: Manifest](value: T): T =
    apply(serialize(value)).extract[T]

  /** Create a patch that applies `this` patch and then `that` patch */
  def andThen(that: JsonPatch): JsonPatch =
    JsonPatch(this.ops ++ that.ops)

  override def toString = pretty(render(toJson))

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
    JsonParser.parse(patch).extract[JsonPatch]

}

private case class RawAdd(path: String, value: JValue) {
  val resolve = Add(pointer.parse(path), value)
}

private case class RawRemove(path: String) {
  val resolve = Remove(pointer.parse(path))
}

private case class RawReplace(path: String, value: JValue) {
  val resolve = Replace(pointer.parse(path), value)
}

private case class RawCopy(from: String, path: String) {
  val resolve = Copy(pointer.parse(from), pointer.parse(path))
}

private case class RawMove(from: String, path: String) {
  val resolve = Move(pointer.parse(from), pointer.parse(path))
}

private case class RawTest(path: String, value: JValue) {
  val resolve = Test(pointer.parse(path), value)
}

/** A patch operation to apply to a Json value */
sealed abstract class Operation {
  val path: Pointer
  def toJson: JObject

  /** Applies this operation to the given Json value */
  def apply(json: String, compacted: Boolean = false): String = {
    val patched = apply(JsonParser.parse(json))
    if(compacted)
      compact(render(patched))
    else
      pretty(render(patched))
  }

  /** Applies this operation to the given Json value */
  def apply(value: JValue): JValue = action(value, path)

  // internals

  // the action to perform in this operation. By default returns an object that is equal
  protected[this] def action(value: JValue, pointer: Pointer): JValue = (value, pointer) match {
    case (_, Nil) =>
      value
    case (JField(name, value), elem :: tl) if name == elem =>
      JField(name, action(value, tl))
    case (JObject(fields), elem :: tl) if value \ elem != JNothing =>
      val fields1 = fields map {
        case JField(name, value) if name == elem =>
          JField(name, action(value, tl))
        case f =>
          f
      }
      JObject(fields1)
    case (JArray(elems), IntIndex(idx) :: tl) =>
      if (idx >= elems.size)
        throw new PatchException("index out of bounds " + idx)
      val elems1 = elems.take(idx) ::: List(action(elems(idx), tl)) ::: elems.drop(idx + 1)
      JArray(elems1)
    case (_, elem :: _) =>
      throw new PatchException("element " + elem + " does not exist in " + pp(value))
  }

}

/** Add (or replace if existing) the pointed element */
final case class Add(path: Pointer, value: JValue) extends Operation {

  lazy val toJson =
    ("op" -> "add") ~
    ("path" -> pointerString(path)) ~
    ("value" -> value)

  override def action(original: JValue, pointer: Pointer): JValue = (original, pointer) match {
    case (_, Nil) =>
      // we are at the root value, simply return the replacement value
      value
    case (JArray(arr), List("-")) =>
      // insert the value at the end of the array
      JArray(arr ::: List(value))
    case (JArray(arr), List(IntIndex(idx))) =>
      if(idx >= arr.size)
        throw new PatchException("Index out of bounds " + idx)
      else
        // insert the value at the specified index
        JArray(arr.take(idx) ::: List(value) ::: arr.drop(idx))
    case (JObject(obj), List(lbl)) =>
      // remove the label if it is present
      val cleaned = obj filter {
        case JField(name, _) => name != lbl
      }
      // insert the new label
      JObject(JField(lbl, value) :: cleaned)
    case _ =>
      super.action(original, pointer)
  }
}

/** Remove the pointed element */
final case class Remove(path: Pointer) extends Operation {

  lazy val toJson =
    ("op" -> "remove") ~
    ("path" -> pointerString(path))

  override def action(original: JValue, pointer: Pointer): JValue =
    (original, pointer) match {
      case (JArray(arr), List(IntIndex(idx))) =>
        if(idx >= arr.size)
          // we know thanks to the extractor that the index cannot be negative
          throw new PatchException("Index out of bounds " + idx)
        else
          // remove the element at the given index
          JArray(arr.take(idx) ::: arr.drop(idx + 1))
      case (JArray(_), List("-")) =>
        // how could we possibly remove an element that appears after the last one?
        throw new PatchException("Index out of bounds -")
      case (JObject(obj), List(lbl)) if original \ lbl != JNothing =>
        // remove the field from the object if present, otherwise, ignore it
        JObject(obj filter { case JField(name, _) => name != lbl })
      case (_, Nil) =>
        throw new PatchException("Cannot delete an empty path")
      case _ =>
        super.action(original, pointer)
    }
}

/** Replace the pointed element by the given value */
final case class Replace(path: Pointer, value: JValue) extends Operation {

  lazy val toJson =
    ("op" -> "replace") ~
    ("path" -> pointerString(path)) ~
    ("value" -> value)

  override def action(original: JValue, pointer: Pointer): JValue =
    (original, pointer) match {
      case (_, Nil) =>
        // simply replace the root value by the replacement value
        value
      case (JArray(arr), List(IntIndex(idx))) =>
        if(idx >= arr.size)
          throw new PatchException("Index out of bounds " + idx)
        else
          JArray(arr.take(idx) ::: List(value) ::: arr.drop(idx + 1))
      case (JArray(_), List("-")) =>
        throw new PatchException("Index out of bounds -")
      case (o @ JObject(obj), List(lbl)) =>
        if(o \ lbl == JNothing)
          throw new PatchException("element " + lbl + " does not exist in " + pp(original))

        val cleaned = obj filter { case JField(name, _) => name != lbl }
        JObject(JField(lbl, value) :: cleaned)
      case _ =>
        super.action(original, pointer)
    }

}

/** Move the pointed element to the new position */
final case class Move(from: Pointer, path: Pointer) extends Operation {

  lazy val toJson =
    ("op" -> "move") ~
    ("from" -> pointerString(from)) ~
    ("path" -> pointerString(path))

  override def apply(original: JValue): JValue = {
    @tailrec
    def prefix(p1: Pointer, p2: Pointer): Boolean = (p1, p2) match {
      case (h1 :: tl1, h2 :: tl2) if h1 == h2 => prefix(tl1, tl2)
      case (Nil, _ :: _)                      => true
      case (_, _)                             => false
    }
    if(prefix(from, path))
      throw new PatchException("The path were to move cannot be a descendent of the from path")

    val cleaned = Remove(from)(original)
    Add(path, pointer.evaluate(original, from))(cleaned)
  }

}

/** Copy the pointed element to the new position */
final case class Copy(from: Pointer, path: Pointer) extends Operation {

  lazy val toJson =
    ("op" -> "copy") ~
    ("from" -> pointerString(from)) ~
    ("path" -> pointerString(path))

  override def apply(original: JValue): JValue =
    Add(path, pointer.evaluate(original, from))(original)
}

/** Test that the pointed element is equal to the given value */
final case class Test(path: Pointer, value: JValue) extends Operation {

  lazy val toJson =
    ("op" -> "test") ~
    ("path" -> pointerString(path)) ~
    ("value" -> value)

  override def apply(original: JValue): JValue = {
    val orig  = JsonPointer.evaluate(original, path)
    if(value != orig)
      throw new PatchException(pp(orig) + " is not equal to " + pp(value))
    else
      original
  }
}
