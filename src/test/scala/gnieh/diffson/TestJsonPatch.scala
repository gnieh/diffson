package gnieh.diffson
package test

import org.scalatest._
import play.api.libs.json._

class TestJsonPatch extends FlatSpec
    with ShouldMatchers
    with TestAddPatch
    with TestRemovePatch
    with TestReplacePatch
    with TestMovePatch {

}

trait TestAddPatch {
  this: TestJsonPatch =>

  "applying an 'add' operation" should "add the field to the object if it does not exist" in {
    Add(pointer.parse("/lbl"), JsNumber(17))(Json.parse("{}")) should be(Json.parse("{ \"lbl\": 17 } "))
  }

  it should "replace the value if the pointer is the root" in {
    Add(pointer.parse(""), JsNumber(17))(Json.parse("[1, 2, 3, 4]")) should be(JsNumber(17))
  }

  it should "replace the field value if it does exist" in {
    Add(pointer.parse("/lbl"), JsNumber(17))(Json.parse("{ \"lbl\": true }")) should be(Json.parse("{ \"lbl\": 17 } "))
  }

  it should "add an element to the array at the given index" in {
    Add(pointer.parse("/1"), JsNumber(17))(Json.parse("[1, 2, 3]")) should be(Json.parse("[1, 17, 2, 3]"))
    Add(pointer.parse("/0"), JsNumber(17))(Json.parse("[1, 2, 3]")) should be(Json.parse("[17, 1, 2, 3]"))
  }

  it should "add an element at the end of the array if the last element is '-'" in {
    Add(pointer.parse("/-"), JsNumber(17))(Json.parse("[1, 2, 3]")) should be(Json.parse("[1, 2, 3, 17]"))
  }

  it should "create a nested field if needed" in {
    Add(pointer.parse("/lbl/lbl"), JsNumber(17))(Json.parse("{ \"lbl\": {} }")) should be(Json.parse("{ \"lbl\": { \"lbl\": 17 } }"))
  }

  it should "throw an error if some element is missing in the middle of the path" in {
    a[PatchException] should be thrownBy { Add(pointer.parse("/lbl/lbl"), JsNumber(17))(Json.parse("{}")) }
  }

  it should "throw an error if adding an element out of the array boundaries" in {
    a[PatchException] should be thrownBy { Add(pointer.parse("/178"), JsNumber(17))(Json.parse("[1, 2]")) }
  }

}

trait TestRemovePatch {
  this: TestJsonPatch =>

  "removing a label of an object" should "result in the object being amputed from this label" in {
    Remove(pointer.parse("/lbl"))(Json.parse("{ \"lbl\": 17, \"toto\": true }")) should be(Json.parse("{ \"toto\": true }"))
  }

  "removing an element of an array" should "result in the array being amputed from this element" in {
    Remove(pointer.parse("/2"))(Json.parse("[1, 2, 3, 4, 5]")) should be(Json.parse("[1, 2, 4, 5]"))
  }

  "removing the '-' element of an array" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy { Remove(pointer.parse("/-"))(Json.parse("[1, 2, 3, 4]")) }
  }

  "removing an element out of the array boundaries" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy { Remove(pointer.parse("/20"))(Json.parse("[1, 2, 3, 4]")) }
  }

  "removing an unknown label from an object" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy { Remove(pointer.parse("/toto"))(Json.parse("{}")) }
  }

  "removing the root" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy { Remove(pointer.parse("/"))(Json.parse("{}")) }
  }
}

trait TestReplacePatch {
  this: TestJsonPatch =>

  "replacing an element in an object" should "result in this element being replaced" in {
    Replace(pointer.parse("/lbl/lbl"), JsNumber(17))(
      Json.parse("""{"lbl": {"lbl": true, "gruik": 1}, "toto": 3}""")) should be(Json.parse("""{"lbl": {"lbl": 17, "gruik": 1}, "toto": 3}"""))
  }

  "replacing an element in an array" should "result in this element being replaced" in {
    Replace(pointer.parse("/3"), JsNumber(17))(Json.parse("[true, false, true, true, true]")) should be(Json.parse("[true, false, true, 17,true]"))
  }

  "replacing the root" should "result in the value being completely replaced" in {
    Replace(pointer.parse(""), JsNumber(17))(Json.parse("[1, 2, 3]")) should be(JsNumber(17))
  }

  "replacing a non existing element in an object" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy { Replace(pointer.parse("/1/lbl"), JsNumber(17))(Json.parse("[1, {}, true]")) }
  }

  "replacing the '-' element of an array" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy { Replace(pointer.parse("/-"), JsNumber(17))(Json.parse("[1, 2, 3, 4]")) }
  }

  "replacing an element out of the array boundaries" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy { Replace(pointer.parse("/20"), JsNumber(17))(Json.parse("[1, 2, 3, 4]")) }
  }

}

trait TestMovePatch {
  this: TestJsonPatch =>

  "moving a value from an object to an array" should "result in the value being added to the array and removed from the object" in {
    Move(pointer.parse("/0/lbl"), pointer.parse("/1/1"))(
      Json.parse("[{ \"lbl\": 17, \"toto\": true }, [1, 2], \"plop\"]")) should be(
        Json.parse("[{ \"toto\": true }, [1, 17, 2], \"plop\"]"))
  }

  "moving a value in a sub element" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy { Move(pointer.parse("/0"), pointer.parse("/0/toto"))(Json.parse("0")) }
  }

  "moving the root" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy { Move(pointer.parse("/"), pointer.parse("/toto"))(Json.parse("0")) }
  }
}
