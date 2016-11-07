package gnieh.diffson
package test

import org.scalatest._

abstract class TestSerialization[JsValue, Instance <: DiffsonInstance[JsValue]](val instance: Instance) extends FlatSpec with ShouldMatchers {

  import instance._
  import provider._

  implicit def boolMarshaller: Marshaller[Boolean]
  implicit def intMarshaller: Marshaller[Int]
  implicit def stringMarshaller: Marshaller[String]
  implicit def testJsonMarshaller: Marshaller[Json]
  implicit def testJsonUnmarshaller: Unmarshaller[Json]

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
                |  "path":"/f/g"
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
                        |  "path":"/f/g"
                        |}]""".stripMargin

  val parsed =
    parseJson(patch)

  val parsedRemember =
    parseJson(patchRemember)

  val json = JsonPatch(
    Replace(Pointer("a"), marshall(6)),
    Remove(Pointer("b")),
    Add(Pointer("c"), marshall("test2")),
    Test(Pointer("d"), marshall(false)),
    Copy(Pointer("c"), Pointer("e")),
    Move(Pointer("d"), Pointer("f", "g"))
  )

  val jsonRemember = JsonPatch(
    Replace(Pointer("a"), marshall(6), Some(marshall(5))),
    Remove(Pointer("b"), Some(marshall("removed value"))),
    Add(Pointer("c"), marshall("test2")),
    Test(Pointer("d"), marshall(false)),
    Copy(Pointer("c"), Pointer("e")),
    Move(Pointer("d"), Pointer("f", "g"))
  )

  "a patch json" should "be correctly deserialized from a Json object" in {
    unmarshall[JsonPatch](parsed) should be(json)
  }

  "a patch object" should "be correctly serialized to a Json object" in {
    marshall(json) should be(parsed)
  }

  "a remembering patch json" should "be correctly deserialized from a Json object" in {
    unmarshall[JsonPatch](parsedRemember) should be(jsonRemember)
  }

  "a remembering patch object" should "be correctly serialized to a Json object" in {
    marshall(jsonRemember) should be(parsedRemember)
  }

  "a patch" should "be applicable to a serializable Scala object if the shape is kept" in {
    val json1 = Json(1, true, "test", List(1, 2, 4))
    val json2 = Json(10, false, "toto", List(1, 2, 3, 4, 5))
    val patch = JsonDiff.diff(json1, json2, false)

    patch(json1) should be(json2)

  }

  "applying a patch" should "raise an exception if it changes the shape" in {
    val json = Json(1, true, "test", Nil)
    val patch = JsonPatch(Replace(Pointer.root, marshall(true)))
    a[Exception] should be thrownBy { patch(json) }
  }

}

case class Json(a: Int, b: Boolean, c: String, d: List[Int])

