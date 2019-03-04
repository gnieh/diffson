package diffson
package jsonpatch
package test

import jsonpointer._

import cats.implicits._

import org.scalatest._

import scala.util.Try

import scala.language.implicitConversions

abstract class TestJsonPatch[Json](implicit Json: Jsony[Json]) extends FlatSpec
  with Matchers {

  def parseJson(s: String): Json

  implicit def intMarshaller(i: Int): Json

  def parsePointer(s: String): Pointer =
    Pointer.parse[Try](s).get

  "applying an 'add' operation" should "add the field to the object if it does not exist" in {
    val op = Add[Json](parsePointer("/lbl"), 17)
    op(parseJson("{}")) should be(parseJson("{ \"lbl\": 17 } "))
  }

  "applying an 'add' operation to /foo/" should "add a value with an empty string as the key" in {
    val op = Add[Json](parsePointer("/foo/"), 17)
    op(parseJson("{ \"foo\": {} }")) should be(parseJson("{ \"foo\": {\"\": 17 } }"))
  }

  it should "replace the value if the pointer is the root" in {
    val op = Add[Json](parsePointer(""), 17)
    op(parseJson("[1, 2, 3, 4]")) should be(17)
  }

  it should "replace the field value if it does exist" in {
    val op = Add[Json](parsePointer("/lbl"), 17)
    op(parseJson("{ \"lbl\": true }")) should be(parseJson("{ \"lbl\": 17 } "))
  }

  it should "add an element to the array at the given index" in {
    val op1 = Add[Json](parsePointer("/1"), 17)
    op1(parseJson("[1, 2, 3]")) should be(parseJson("[1, 17, 2, 3]"))
    val op2 = Add[Json](parsePointer("/0"), 17)
    op2(parseJson("[1, 2, 3]")) should be(parseJson("[17, 1, 2, 3]"))
  }

  it should "add an element at the end of the array if the last element is '-'" in {
    val op = Add[Json](parsePointer("/-"), 17)
    op(parseJson("[1, 2, 3]")) should be(parseJson("[1, 2, 3, 17]"))
  }

  it should "create a nested field if needed" in {
    val op = Add[Json](parsePointer("/lbl/lbl"), 17)
    op(parseJson("{ \"lbl\": {} }")) should be(parseJson("{ \"lbl\": { \"lbl\": 17 } }"))
  }

  it should "throw an error if some element is missing in the middle of the path" in {
    a[PatchException] should be thrownBy {
      val op = Add[Json](parsePointer("/lbl/lbl"), 17)
      op(parseJson("{}"))
    }
  }

  it should "throw an error if adding an element out of the array boundaries" in {
    a[PatchException] should be thrownBy {
      val op = Add[Json](parsePointer("/178"), 17)
      op(parseJson("[1, 2]"))
    }
  }

  "removing a label of an object" should "result in the object being amputed from this label" in {
    val op = Remove[Json](parsePointer("/lbl"))
    op(parseJson("{ \"lbl\": 17, \"toto\": true }")) should be(parseJson("{ \"toto\": true }"))
  }

  "removing an element of an array" should "result in the array being amputed from this element" in {
    val op = Remove[Json](parsePointer("/2"))
    op(parseJson("[1, 2, 3, 4, 5]")) should be(parseJson("[1, 2, 4, 5]"))
  }

  "removing the '-' element of an array" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove[Json](parsePointer("/-"))
      op(parseJson("[1, 2, 3, 4]"))
    }
  }

  "removing an element out of the array boundaries" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove[Json](parsePointer("/20"))
      op(parseJson("[1, 2, 3, 4]"))
    }
  }

  "removing an unknown label from an object" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove[Json](parsePointer("/toto"))
      op(parseJson("{}"))
    }
  }

  "removing the root" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove[Json](parsePointer("/"))
      op(parseJson("{}"))
    }
  }

  "replacing an element in an object" should "result in this element being replaced" in {
    val op = Replace[Json](parsePointer("/lbl/lbl"), 17)
    op(parseJson("""{"lbl": {"lbl": true, "gruik": 1}, "toto": 3}""")) should be(parseJson("""{"lbl": {"lbl": 17, "gruik": 1}, "toto": 3}"""))
  }

  "replacing an element in an array" should "result in this element being replaced" in {
    val op = Replace[Json](parsePointer("/3"), 17)
    op(parseJson("[true, false, true, true, true]")) should be(parseJson("[true, false, true, 17,true]"))
  }

  "replacing the root" should "result in the value being completely replaced" in {
    val op = Replace[Json](parsePointer(""), 17)
    op(parseJson("[1, 2, 3]")) should be(17)
  }

  "replacing a non existing element in an object" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Replace[Json](parsePointer("/1/lbl"), 17)
      op(parseJson("[1, {}, true]"))
    }
  }

  "replacing the '-' element of an array" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Replace[Json](parsePointer("/-"), 17)
      op(parseJson("[1, 2, 3, 4]"))
    }
  }

  "replacing an element out of the array boundaries" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Replace[Json](parsePointer("/20"), 17)
      op(parseJson("[1, 2, 3, 4]"))
    }

    a[PatchException] should be thrownBy {
      val op = Replace[Json](parsePointer("/array/3/sub1"), 17)
      op(parseJson("{\"array\":[\"bar1\",\"bar2\",{\"sub1\":\"bar3\"}]}"))
    }
  }

  "moving a value from an object to an array" should "result in the value being added to the array and removed from the object" in {
    val op = Move(parsePointer("/0/lbl"), parsePointer("/1/1"))
    op(parseJson("[{ \"lbl\": 17, \"toto\": true }, [1, 2], \"plop\"]")) should be(
      parseJson("[{ \"toto\": true }, [1, 17, 2], \"plop\"]"))
  }

  "moving a value in a sub element" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Move(parsePointer("/0"), parsePointer("/0/toto"))
      op(parseJson("0"))
    }
  }

  "moving the root" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Move(parsePointer("/"), parsePointer("/toto"))
      op(parseJson("0"))
    }
  }
}
