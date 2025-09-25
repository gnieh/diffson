package diffson
package jsonpatch

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import diffson.jsonpatch.JsonDiff
import diffson.lcs.Patience

abstract class TestObjectDiff[J](implicit J: Jsony[J]) extends AnyFlatSpec with Matchers {

  implicit val lcsalg: Patience[J] = new Patience[J]

  val diff = new JsonDiff[J](false, false)

  "a wide object diffed with an empty one" should "not cause stack overflows" in {
    val json1 = J.makeObject((1 to 10000).map(i => s"key$i" -> J.Null).toMap)
    val json2 = J.makeObject(Map.empty)

    diff.diff(json1, json2)
  }

  "a wide object diffed with itself" should "not cause stack overflows" in {
    val json1 = J.makeObject((1 to 10000).map(i => s"key$i" -> J.Null).toMap)

    diff.diff(json1, json1)
  }

}
