package diffson
package jsonmergepatch

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Try
import org.scalatest.matchers.should.Matchers

abstract class TestJsonMergePatch[Json](implicit Json: Jsony[Json]) extends AnyFlatSpec with Matchers with TestProtocol[Json] {

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
      val p = parseMergePatch(patch)
      val res = parseJson(result)
      p[Try](orig).get should be(res)
    }

}
