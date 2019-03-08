package diffson
package jsonmergepatch
package test

import cats._

import org.scalatest._

import scala.language.implicitConversions

abstract class TestJsonMergeDiff[Json](implicit Json: Jsony[Json]) extends FlatSpec with Matchers {

  def parseJson(s: String): Json

  implicit def boolMarshaller(b: Boolean): Json
  implicit def intMarshaller(i: Int): Json
  implicit def stringMarshaller(s: String): Json
  implicit def jsonEq: Eq[Json]

  "a diff" should "be empty if created between two equal objects" in {
    val json = parseJson("""{"a": true}""")
    diff(json, json) should be(JsonMergePatch.Object(Map()))
  }

  it should "be a simple replacement if the two values are completely different" in {
    diff(parseJson("true"), parseJson("13")) should be(JsonMergePatch.Value(13: Json))
  }

  it should "be generated correctly for nested structures" in {
    val json1 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json2 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}}""")
    diff(json1, json2) should be(JsonMergePatch.Object(Map("b" -> JsObject(Map("b" -> (43: Json))))))
  }

}
