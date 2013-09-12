package gnieh.diffson
package test

import org.scalatest._

import net.liftweb.json._

class TestSerialization extends FlatSpec with ShouldMatchers {

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
                |  "from":"/c"
                |  "path":"/e"
                |},{
                |  "op":"move",
                |  "from":"/d",
                |  "path":"/f"
                |}]""".stripMargin

  val parsed =
    JsonParser.parse(patch)

  val json = JsonPatch(
    Replace(List("a"), JInt(6)),
    Remove(List("b")),
    Add(List("c"), JString("test2")),
    Test(List("d"), JBool(false)),
    Copy(List("c"), List("e")),
    Move(List("d"), List("f"))
  )

  "a patch json" should "be correctly deserialized from a Json object" in {
    parsed.extract[JsonPatch] should be(json)
  }

  "a patch object" should "be correctly serialized to a Json object" in {
    Extraction.decompose(json) should be(parsed)
  }

}
