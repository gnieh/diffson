package gnieh.diffson
package test

import org.scalatest._

abstract class TestJsonPointer[JsValue, Instance <: DiffsonInstance[JsValue]](val instance: Instance) extends FlatSpec with Matchers {

  import instance._
  import provider._

  import JsonPointer._

  implicit def boolUnmarshaller: Unmarshaller[Boolean]
  implicit def intUnmarshaller: Unmarshaller[Int]

  "an empty string" should "be parsed as an empty pointer" in {
    JsonPointer.parse("").path should be(Pointer.Root)
  }

  "the root pointer" should "be parsed as the pointer to empty element at root" in {
    JsonPointer.parse("/").path should be(Pointer(""))
  }

  "a pointer string with one chunk" should "be parsed as a pointer with one element" in {
    JsonPointer.parse("/test").path should be(Pointer("test"))
  }

  "occurrences of ~0" should "be replaced by occurrences of ~" in {
    JsonPointer.parse("/~0/test/~0~0plop").path should be(Pointer("~", "test", "~~plop"))
  }

  "occurrences of ~1" should "be replaced by occurrences of /" in {
    JsonPointer.parse("/test~1/~1/plop").path should be(Pointer("test/", "/", "plop"))
  }

  "occurrences of ~" should "be directly followed by either 0 or 1" in {
    a[PointerException] should be thrownBy { JsonPointer.parse("/~") }
    a[PointerException] should be thrownBy { JsonPointer.parse("/~3") }
    a[PointerException] should be thrownBy { JsonPointer.parse("/~d") }
  }

  "a non empty pointer" should "start with a /" in {
    a[PointerException] should be thrownBy { JsonPointer.parse("test") }
  }

  "a pointer to a label" should "be evaluated to the label value if it is one level deep" in {
    unmarshall[Boolean](JsonPointer.parse("/label").evaluate(parseJson("{\"label\": true}"))) should be(true)
  }

  it should "be evaluated to the end label value if it is several levels deep" in {
    unmarshall[Int](JsonPointer.parse("/l1/l2/l3").evaluate(parseJson("""{"l1": {"l2": { "l3": 17 } } }"""))) should be(17)
  }

  it should "be evaluated to nothing if the final element is unknown" in {
    JsonPointer.parse("/lbl").evaluate(parseJson("{}")) should be(JsNull)
  }

  it should "produce an error if there is an unknown element in the middle of the pointer" in {
    a[PointerException] should be thrownBy { JsonPointer.parse("/lbl/test").evaluate(parseJson("{}")) }
  }

  "a pointer to an array element" should "be evaluated to the value at the given index" in {
    unmarshall[Int](JsonPointer.parse("/1").evaluate(parseJson("[1, 2, 3]"))) should be(2)
    unmarshall[Int](JsonPointer.parse("/lbl/4").evaluate(parseJson("{ \"lbl\": [3, 7, 5, 4, 7] }"))) should be(7)
  }

  it should "produce an error if it is out of the array bounds" in {
    a[PointerException] should be thrownBy { JsonPointer.parse("/4").evaluate(parseJson("[1]")) }
  }

  it should "produce an error if it is the '-' element" in {
    a[PointerException] should be thrownBy { JsonPointer.parse("/-").evaluate(parseJson("[1]")) }
  }

}
