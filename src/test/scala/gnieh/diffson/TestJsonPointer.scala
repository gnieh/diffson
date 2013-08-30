package gnieh.diffson
package test

import org.scalatest._

import net.liftweb.json._

class TestJsonPointer extends FlatSpec with ShouldMatchers {

  val pointer = new JsonPointer()

  "an empty string" should "be parsed as an empty pointer" in {
    pointer.parse("") should be(Nil)
  }

  "the root pointer" should "be parsed as an empty pointer" in {
    pointer.parse("/") should be(Nil)
  }

  "a pointer string with one chunk" should "be parsed as a pointer with one element" in {
    pointer.parse("/test") should be(List("test"))
  }

  "occurrences of ~0" should "be replaced by occurrences of ~" in {
    pointer.parse("/~0/test/~0~0plop") should be(List("~", "test", "~~plop"))
  }

  "occurrences of ~1" should "be replaced by occurrences of /" in {
    pointer.parse("/test~1/~1/plop") should be(List("test/", "/", "plop"))
  }

  "occurrences of ~" should "be directly followed by either 0 or 1" in {
    evaluating { pointer.parse("/~") } should produce[PointerException]
    evaluating { pointer.parse("/~3") } should produce[PointerException]
    evaluating { pointer.parse("/~d") } should produce[PointerException]
  }

  "a non empty pointer" should "start with a /" in {
    evaluating { pointer.parse("test") } should produce[PointerException]
  }

  "a pointer to a label" should "be evaluated to the label value if it is one level deep" in {
    pointer.evaluate("{\"label\": true}", "/label") should be(JBool(true))
  }

  it should "be evaluated to the end label value if it is several levels deep" in {
    pointer.evaluate("""{"l1": {"l2": { "l3": 17 } } }""", "/l1/l2/l3") should be(JInt(17))
  }

  it should "be evaluated to nothing if the final element is unknown" in {
    pointer.evaluate("{}", "/lbl") should be(JNothing)
  }

  it should "produce an error if there is an unknown element in the middle of the pointer" in {
    evaluating { pointer.evaluate("{}", "/lbl/test") } should produce[PointerException]
  }

  "a pointer to an array element" should "be evaluated to the value at the given index" in {
    pointer.evaluate("[1, 2, 3]", "/1") should be(JInt(2))
    pointer.evaluate("{ \"lbl\": [3, 7, 5, 4, 7] }", "/lbl/4") should be(JInt(7))
  }

  it should "produce an error if it is out of the array bounds" in {
    evaluating { pointer.evaluate("[1]", "/4") } should produce[PointerException]
  }

  it should "produce an error if it is the '-' element" in {
    evaluating { pointer.evaluate("[1]", "/-") } should produce[PointerException]
  }

}
