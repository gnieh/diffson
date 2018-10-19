package gnieh.diffson
package test

import org.scalatest._

abstract class TestJsonMergePatch[JsValue, Instance <: DiffsonInstance[JsValue]](val instance: Instance) extends FlatSpec with Matchers {

  import instance._
  import provider._

  val samples = List(
    ("""{"a":"b"}""", """{"a":"c"}""", """{"a":"c"}"""),
    ("""{"a":"b"}""", """{"b":"c"}""", """{"a":"b","b":"c"}"""),
    ("""{"a":"b"}""", """{"a":null}""", """{}"""),
    ("""{"a":"b","b":"c"}""", """{"a":null}""", """{"b":"c"}"""),
    ("""{"a":["b"]}""", """{"a":"c"}""", """{"a":"c"}"""),
    ("""{"a":"c"}""", """{"a":["b"]}""", """{"a":["b"]}"""),
    ("""{"a": {"b": "c"}}""", """{"a": {"b": "d","c": null}}""", """{"a": {"b": "d"}}"""),
    ("""{"a": [{"b":"c"}]}""", """{"a": [1]}""", """{"a": [1]}"""),
    ("""["a","b"]""", """["c","d"]""", """["c","d"]"""),
    ("""{"a":"b"}""", """["c"]""", """["c"]"""),
    ("""{"a":"foo"}""", """null""", """null"""),
    ("""{"a":"foo"}""", """"bar"""", """"bar""""),
    ("""{"e":null}""", """{"a":1}""", """{"e":null,"a":1}"""),
    ("""[1,2]""", """{"a":"b","c":null}""", """{"a":"b"}"""),
    ("""{}""", """{"a":{"bb":{"ccc": null}}}""", """{"a":{"bb":{}}}"""))

  for ((original, patch, result) <- samples)
    s"patching $original with $patch" should s"result in $result" in {
      val orig = parseJson(original)
      val p = JsonMergePatch.parse(patch)
      val res = parseJson(result)
      p(orig) should be(res)
    }

}
