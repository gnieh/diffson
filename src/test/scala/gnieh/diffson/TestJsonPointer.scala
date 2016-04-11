package gnieh.diffson
package test

import sprayJson._

import spray.json._

import org.scalatest._

class TestJsonPointer extends FlatSpec with ShouldMatchers {

  "an empty string" should "be parsed as an empty pointer" in {
    pointer.parse("") should be(Pointer.empty)
  }

  "the root pointer" should "be parsed as the pointer to empty element at root" in {
    pointer.parse("/") should be(Pointer(""))
  }

  "a pointer string with one chunk" should "be parsed as a pointer with one element" in {
    pointer.parse("/test") should be(Pointer("test"))
  }

  "occurrences of ~0" should "be replaced by occurrences of ~" in {
    pointer.parse("/~0/test/~0~0plop") should be(Pointer("~", "test", "~~plop"))
  }

  "occurrences of ~1" should "be replaced by occurrences of /" in {
    pointer.parse("/test~1/~1/plop") should be(Pointer("test/", "/", "plop"))
  }

  "occurrences of ~" should "be directly followed by either 0 or 1" in {
    a[PointerException] should be thrownBy { pointer.parse("/~") }
    a[PointerException] should be thrownBy { pointer.parse("/~3") }
    a[PointerException] should be thrownBy { pointer.parse("/~d") }
  }

  "a non empty pointer" should "start with a /" in {
    a[PointerException] should be thrownBy { pointer.parse("test") }
  }

  "a pointer to a label" should "be evaluated to the label value if it is one level deep" in {
    pointer.evaluate("{\"label\": true}", "/label") should be(JsBoolean(true))
  }

  it should "be evaluated to the end label value if it is several levels deep" in {
    pointer.evaluate("""{"l1": {"l2": { "l3": 17 } } }""", "/l1/l2/l3") should be(JsNumber(17))
  }

  it should "be evaluated to nothing if the final element is unknown" in {
    pointer.evaluate("{}", "/lbl") should be(JsNull)
  }

  it should "produce an error if there is an unknown element in the middle of the pointer" in {
    a[PointerException] should be thrownBy { pointer.evaluate("{}", "/lbl/test") }
  }

  "a pointer to an array element" should "be evaluated to the value at the given index" in {
    pointer.evaluate("[1, 2, 3]", "/1") should be(JsNumber(2))
    pointer.evaluate("{ \"lbl\": [3, 7, 5, 4, 7] }", "/lbl/4") should be(JsNumber(7))
  }

  it should "produce an error if it is out of the array bounds" in {
    a[PointerException] should be thrownBy { pointer.evaluate("[1]", "/4") }
  }

  it should "produce an error if it is the '-' element" in {
    a[PointerException] should be thrownBy { pointer.evaluate("[1]", "/-") }
  }

}
