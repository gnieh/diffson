package gnieh.diffson
package test

import org.scalatest._

abstract class TestJsonMergeDiff[JsValue, Instance <: DiffsonInstance[JsValue]](val instance: Instance) extends FlatSpec with Matchers {

  import instance._
  import provider._

  implicit def boolMarshaller: Marshaller[Boolean]
  implicit def intMarshaller: Marshaller[Int]
  implicit def stringMarshaller: Marshaller[String]

  import JsonMergeDiff._

  "a diff" should "be empty if created between two equal objects" in {
    val json = parseJson("""{"a": true}""")
    diff(json, json) should be(JsonMergePatch.Object(Map()))
  }

  it should "be a simple replacement if the two values are completely different" in {
    diff(parseJson("true"), parseJson("13")) should be(JsonMergePatch.Value(marshall(13)))
  }

  it should "be generated correctly for nested structures" in {
    val json1 = parseJson("""{"a": 3, "b": {"a": true }}""")
    val json2 = parseJson("""{"a": 3, "b": {"a": true, "b": 43}}""")
    diff(json1, json2) should be(JsonMergePatch.Object(Map("b" -> JsObject(Map("b" -> marshall(43))))))
  }

}
