package diffson
package jsonpatch

import jsonpointer._

import cats.implicits._

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Try

import scala.language.implicitConversions

abstract class TestJsonPatch[Json](implicit Json: Jsony[Json]) extends AnyFlatSpec
  with Matchers with TestProtocol[Json] {

  "applying an 'add' operation" should "add the field to the object if it does not exist" in {
    val op = Add[Json](parsePointer("/lbl"), 17)
    op[Try](parseJson("{}")).get should be(parseJson("{ \"lbl\": 17 } "))
  }

  "applying an 'add' operation to /foo/" should "add a value with an empty string as the key" in {
    val op = Add[Json](parsePointer("/foo/"), 17)
    op[Try](parseJson("{ \"foo\": {} }")).get should be(parseJson("{ \"foo\": {\"\": 17 } }"))
  }

  it should "replace the value if the pointer is the root" in {
    val op = Add[Json](parsePointer(""), 17)
    op[Try](parseJson("[1, 2, 3, 4]")).get should be(17: Json)
  }

  it should "replace the field value if it does exist" in {
    val op = Add[Json](parsePointer("/lbl"), 17)
    op[Try](parseJson("{ \"lbl\": true }")).get should be(parseJson("{ \"lbl\": 17 } "))
  }

  it should "add an element to the array at the given index" in {
    val op1 = Add[Json](parsePointer("/1"), 17)
    op1[Try](parseJson("[1, 2, 3]")).get should be(parseJson("[1, 17, 2, 3]"))
    val op2 = Add[Json](parsePointer("/0"), 17)
    op2[Try](parseJson("[1, 2, 3]")).get should be(parseJson("[17, 1, 2, 3]"))
  }

  it should "add an element at the end of the array if the last element is '-'" in {
    val op = Add[Json](parsePointer("/-"), 17)
    op[Try](parseJson("[1, 2, 3]")).get should be(parseJson("[1, 2, 3, 17]"))
  }

  it should "create a nested field if needed" in {
    val op = Add[Json](parsePointer("/lbl/lbl"), 17)
    op[Try](parseJson("{ \"lbl\": {} }")).get should be(parseJson("{ \"lbl\": { \"lbl\": 17 } }"))
  }

  it should "throw an error if some element is missing in the middle of the path" in {
    a[PatchException] should be thrownBy {
      val op = Add[Json](parsePointer("/lbl/lbl"), 17)
      op[Try](parseJson("{}")).get
    }
  }

  it should "throw an error if adding an element out of the array boundaries" in {
    a[PatchException] should be thrownBy {
      val op = Add[Json](parsePointer("/178"), 17)
      op[Try](parseJson("[1, 2]")).get
    }
  }

  "removing a label of an object" should "result in the object being amputed from this label" in {
    val op = Remove[Json](parsePointer("/lbl"))
    op[Try](parseJson("{ \"lbl\": 17, \"toto\": true }")).get should be(parseJson("{ \"toto\": true }"))
  }

  "removing an element of an array" should "result in the array being amputed from this element" in {
    val op = Remove[Json](parsePointer("/2"))
    op[Try](parseJson("[1, 2, 3, 4, 5]")).get should be(parseJson("[1, 2, 4, 5]"))
  }

  "removing the '-' element of an array" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove[Json](parsePointer("/-"))
      op[Try](parseJson("[1, 2, 3, 4]")).get
    }
  }

  "removing an element out of the array boundaries" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove[Json](parsePointer("/20"))
      op[Try](parseJson("[1, 2, 3, 4]")).get
    }
  }

  "removing an unknown label from an object" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove[Json](parsePointer("/toto"))
      op[Try](parseJson("{}")).get
    }
  }

  "removing the root" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Remove[Json](parsePointer("/"))
      op[Try](parseJson("{}")).get
    }
  }

  "replacing an element in an object" should "result in this element being replaced" in {
    val op = Replace[Json](parsePointer("/lbl/lbl"), 17)
    op[Try](parseJson("""{"lbl": {"lbl": true, "gruik": 1}, "toto": 3}""")).get should be(parseJson("""{"lbl": {"lbl": 17, "gruik": 1}, "toto": 3}"""))
  }

  "replacing an element in an array" should "result in this element being replaced" in {
    val op = Replace[Json](parsePointer("/3"), 17)
    op[Try](parseJson("[true, false, true, true, true]")).get should be(parseJson("[true, false, true, 17,true]"))
  }

  "replacing the root" should "result in the value being completely replaced" in {
    val op = Replace[Json](parsePointer(""), 17)
    op[Try](parseJson("[1, 2, 3]")).get should be(17: Json)
  }

  "replacing a non existing element in an object" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Replace[Json](parsePointer("/1/lbl"), 17)
      op[Try](parseJson("[1, {}, true]")).get
    }
  }

  "replacing the '-' element of an array" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Replace[Json](parsePointer("/-"), 17)
      op[Try](parseJson("[1, 2, 3, 4]")).get
    }
  }

  "replacing an element out of the array boundaries" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Replace[Json](parsePointer("/20"), 17)
      op[Try](parseJson("[1, 2, 3, 4]")).get
    }

    a[PatchException] should be thrownBy {
      val op = Replace[Json](parsePointer("/array/3/sub1"), 17)
      op[Try](parseJson("{\"array\":[\"bar1\",\"bar2\",{\"sub1\":\"bar3\"}]}")).get
    }
  }

  "moving a value from an object to an array" should "result in the value being added to the array and removed from the object" in {
    val op = Move(parsePointer("/0/lbl"), parsePointer("/1/1"))
    op[Try](parseJson("[{ \"lbl\": 17, \"toto\": true }, [1, 2], \"plop\"]")).get should be(
      parseJson("[{ \"toto\": true }, [1, 17, 2], \"plop\"]"))
  }

  "moving a value in a sub element" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Move(parsePointer("/0"), parsePointer("/0/toto"))
      op[Try](parseJson("0")).get
    }
  }

  "moving the root" should "result in an error being thrown" in {
    a[PatchException] should be thrownBy {
      val op = Move(parsePointer(""), parsePointer("/toto"))
      op[Try](parseJson("0")).get
    }
  }
}
