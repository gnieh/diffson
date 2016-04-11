package gnieh.diffson
package test

import sprayJson._

import org.scalatest._

import spray.json._

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
    val op = Add(pointer.parse("/lbl"), JsNumber(17))
    op(JsonParser("{}")) should be(JsonParser("{ \"lbl\": 17 } "))
  }

  it should "replace the value if the pointer is the root" in {
    val op = Add(pointer.parse(""), JsNumber(17))
    op(JsonParser("[1, 2, 3, 4]")) should be(JsNumber(17))
  }

  it should "replace the field value if it does exist" in {
    val op = Add(pointer.parse("/lbl"), JsNumber(17))
    op(JsonParser("{ \"lbl\": true }")) should be(JsonParser("{ \"lbl\": 17 } "))
  }

  it should "add an element to the array at the given index" in {
    val op1 = Add(pointer.parse("/1"), JsNumber(17))
    op1(JsonParser("[1, 2, 3]")) should be(JsonParser("[1, 17, 2, 3]"))
    val op2 = Add(pointer.parse("/0"), JsNumber(17))
    op2(JsonParser("[1, 2, 3]")) should be(JsonParser("[17, 1, 2, 3]"))
  }

  it should "add an element at the end of the array if the last element is '-'" in {
    val op = Add(pointer.parse("/-"), JsNumber(17))
    op(JsonParser("[1, 2, 3]")) should be(JsonParser("[1, 2, 3, 17]"))
  }

  it should "create a nested field if needed" in {
    val op = Add(pointer.parse("/lbl/lbl"), JsNumber(17))
    op(JsonParser("{ \"lbl\": {} }")) should be(JsonParser("{ \"lbl\": { \"lbl\": 17 } }"))
  }

  it should "throw an error if some element is missing in the middle of the path" in {
    a[PatchException] should be thrownBy {
      val op = Add(pointer.parse("/lbl/lbl"), JsNumber(17))
      op(JsonParser("{}"))
    }
  }

  it should "throw an error if adding an element out of the array boundaries" in {
    a[PatchException] should be thrownBy {
      val op = Add(pointer.parse("/178"), JsNumber(17))
      op(JsonParser("[1, 2]"))
    }
  }

}

trait TestRemovePatch {
  this: TestJsonPatch =>

  "removing a label of an object" should "result in the object being amputed from this label" in {
    val op = Remove(pointer.parse("/lbl"))
    op(JsonParser("{ \"lbl\": 17, \"toto\": true }")) should be(JsonParser("{ \"toto\": true }"))
  }

  "removing an element of an array" should "result in the array being amputed from this element" in {
    val op = Remove(pointer.parse("/2"))
    op(JsonParser("[1, 2, 3, 4, 5]")) should be(JsonParser("[1, 2, 4, 5]"))
  }

  "removing the '-' element of an array" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove(pointer.parse("/-"))
      op(JsonParser("[1, 2, 3, 4]"))
    }
  }

  "removing an element out of the array boundaries" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove(pointer.parse("/20"))
      op(JsonParser("[1, 2, 3, 4]"))
    }
  }

  "removing an unknown label from an object" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove(pointer.parse("/toto"))
      op(JsonParser("{}"))
    }
  }

  "removing the root" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove(pointer.parse("/"))
      op(JsonParser("{}"))
    }
  }
}

trait TestReplacePatch {
  this: TestJsonPatch =>

  "replacing an element in an object" should "result in this element being replaced" in {
    val op = Replace(pointer.parse("/lbl/lbl"), JsNumber(17))
    op(JsonParser("""{"lbl": {"lbl": true, "gruik": 1}, "toto": 3}""")) should be(JsonParser("""{"lbl": {"lbl": 17, "gruik": 1}, "toto": 3}"""))
  }

  "replacing an element in an array" should "result in this element being replaced" in {
    val op = Replace(pointer.parse("/3"), JsNumber(17))
    op(JsonParser("[true, false, true, true, true]")) should be(JsonParser("[true, false, true, 17,true]"))
  }

  "replacing the root" should "result in the value being completely replaced" in {
    val op = Replace(pointer.parse(""), JsNumber(17))
    op(JsonParser("[1, 2, 3]")) should be(JsNumber(17))
  }

  "replacing a non existing element in an object" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Replace(pointer.parse("/1/lbl"), JsNumber(17))
      op(JsonParser("[1, {}, true]"))
    }
  }

  "replacing the '-' element of an array" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Replace(pointer.parse("/-"), JsNumber(17))
      op(JsonParser("[1, 2, 3, 4]"))
    }
  }

  "replacing an element out of the array boundaries" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Replace(pointer.parse("/20"), JsNumber(17))
      op(JsonParser("[1, 2, 3, 4]"))
    }
  }

}

trait TestMovePatch {
  this: TestJsonPatch =>

  "moving a value from an object to an array" should "result in the value being added to the array and removed from the object" in {
    val op = Move(pointer.parse("/0/lbl"), pointer.parse("/1/1"))
    op(JsonParser("[{ \"lbl\": 17, \"toto\": true }, [1, 2], \"plop\"]")) should be(
      JsonParser("[{ \"toto\": true }, [1, 17, 2], \"plop\"]"))
  }

  "moving a value in a sub element" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Move(pointer.parse("/0"), pointer.parse("/0/toto"))
      op(JsonParser("0"))
    }
  }

  "moving the root" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Move(pointer.parse("/"), pointer.parse("/toto"))
      op(JsonParser("0"))
    }
  }
}
