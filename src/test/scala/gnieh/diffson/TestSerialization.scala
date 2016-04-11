package gnieh.diffson
package test

import sprayJson._

import org.scalatest._

import spray.json._

class TestSerialization extends FlatSpec with ShouldMatchers {

  import DiffsonProtocol._

  implicit val testJsonFormat = jsonFormat4(Json)

  val patch = """[{
                |  "op":"replace",
                |  "path":"/a",
                |  "value":6
                |},{
                |  "op":"remove",
                |  "path":"/b"
                |},{
                |  "op":"add",
                |  "path":"/c",
                |  "value":"test2"
                |},{
                |  "op":"test",
                |  "path":"/d",
                |  "value":false
                |},{
                |  "op":"copy",
                |  "from":"/c",
                |  "path":"/e"
                |},{
                |  "op":"move",
                |  "from":"/d",
                |  "path":"/f"
                |}]""".stripMargin

  val patchRemember = """[{
                        |  "op":"replace",
                        |  "path":"/a",
                        |  "value":6,
                        |  "old": 5
                        |},{
                        |  "op":"remove",
                        |  "path":"/b",
                        |  "old": "removed value"
                        |},{
                        |  "op":"add",
                        |  "path":"/c",
                        |  "value":"test2"
                        |},{
                        |  "op":"test",
                        |  "path":"/d",
                        |  "value":false
                        |},{
                        |  "op":"copy",
                        |  "from":"/c",
                        |  "path":"/e"
                        |},{
                        |  "op":"move",
                        |  "from":"/d",
                        |  "path":"/f"
                        |}]""".stripMargin

  val parsed =
    JsonParser(patch)

  val parsedRemember =
    JsonParser(patchRemember)

  val json = JsonPatch(
    Replace(Pointer("a"), JsNumber(6)),
    Remove(Pointer("b")),
    Add(Pointer("c"), JsString("test2")),
    Test(Pointer("d"), JsBoolean(false)),
    Copy(Pointer("c"), Pointer("e")),
    Move(Pointer("d"), Pointer("f"))
  )

  val jsonRemember = JsonPatch(
    Replace(Pointer("a"), JsNumber(6), Some(JsNumber(5))),
    Remove(Pointer("b"), Some(JsString("removed value"))),
    Add(Pointer("c"), JsString("test2")),
    Test(Pointer("d"), JsBoolean(false)),
    Copy(Pointer("c"), Pointer("e")),
    Move(Pointer("d"), Pointer("f"))
  )

  "a patch json" should "be correctly deserialized from a Json object" in {
    parsed.convertTo[JsonPatch] should be(json)
  }

  "a patch object" should "be correctly serialized to a Json object" in {
    json.toJson should be(parsed)
  }

  "a remembering patch json" should "be correctly deserialized from a Json object" in {
    parsedRemember.convertTo[JsonPatch] should be(jsonRemember)
  }

  "a remembering patch object" should "be correctly serialized to a Json object" in {
    jsonRemember.toJson should be(parsedRemember)
  }

  "a pacth" should "be applicable to a serializable Scala object if the shape is kept" in {
    val json1 = Json(1, true, "test", List(1, 2, 4))
    val json2 = Json(10, false, "toto", List(1, 2, 3, 4, 5))
    val patch = JsonDiff.diff(json1, json2, false)

    patch(json1) should be(json2)

  }

  "applying a patch" should "raise an exception if it changes the shape" in {
    val json = Json(1, true, "test", Nil)
    val patch = JsonPatch(Replace(Pointer.root, JsBoolean(true)))
    a[DeserializationException] should be thrownBy { patch(json) }
  }

}

case class Json(a: Int, b: Boolean, c: String, d: List[Int])

