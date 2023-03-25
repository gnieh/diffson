package diffson
package mongoupdate

import cats.Eq
import weaver._

import lcs._
import lcsdiff._

abstract class MongoDiffSpec[Update: Eq, Bson](implicit Updates: Updates[Update, Bson], Jsony: Jsony[Bson])
    extends SimpleIOSuite {

  implicit val lcs = new Patience[Bson]

  def int(i: Int): Bson

  def string(s: String): Bson

  def doc(value: Bson): Bson =
    Jsony.makeObject(Map("value" -> value))

  pureTest("append to empty array") {
    val source = doc(Jsony.makeArray(Vector()))
    val target = doc(Jsony.makeArray(Vector(string("a"), string("b"), string("c"), string("d"))))

    val d = source.diff(target)

    expect.eql(Updates.pushEach(Updates.empty, "value", List(string("a"), string("b"), string("c"), string("d"))), d)
  }

  pureTest("append to array") {

    val source = doc(Jsony.makeArray(Vector(string("a"), string("b"))))
    val target = doc(Jsony.makeArray(Vector(string("a"), string("b"), string("c"), string("d"))))

    val d = source.diff(target)

    expect.eql(Updates.pushEach(Updates.empty, "value", List(string("c"), string("d"))), d)
  }

  pureTest("push in the middle of an array") {
    val source = doc(Jsony.makeArray(Vector(string("a"), string("b"), string("f"))))
    val target =
      doc(Jsony.makeArray(Vector(string("a"), string("b"), string("c"), string("d"), string("e"), string("f"))))

    val d = source.diff(target)

    expect.eql(Updates.pushEach(Updates.empty, "value", 2, List(string("c"), string("d"), string("e"))), d)
  }

  pureTest("push and modify") {
    val source = doc(Jsony.makeArray(Vector(string("a"), string("b"), string("f"))))
    val target =
      doc(Jsony.makeArray(Vector(string("x"), string("b"), string("c"), string("d"), string("e"), string("f"))))

    val d = source.diff(target)

    expect.eql(Updates.set(
                 Updates.empty,
                 "value",
                 Jsony.makeArray(Vector(string("x"), string("b"), string("c"), string("d"), string("e"), string("f")))),
               d)
  }

  pureTest("modify in place") {
    val source = doc(Jsony.makeArray(Vector(string("a"), string("e"), string("c"), string("f"))))
    val target =
      doc(Jsony.makeArray(Vector(string("a"), string("b"), string("c"), string("d"))))

    val d = source.diff(target)

    expect.eql(Updates.set(Updates.set(Updates.empty, "value.1", string("b")), "value.3", string("d")), d)
  }

  pureTest("delete elements of an array") {
    val source =
      doc(Jsony.makeArray(Vector(string("a"), string("b"), string("c"), string("d"), string("e"), string("f"))))
    val target = doc(Jsony.makeArray(Vector(string("a"), string("b"), string("f"))))

    val d = source.diff(target)

    expect.eql(Updates.set(Updates.empty, "value", Jsony.makeArray(Vector(string("a"), string("b"), string("f")))), d)
  }

}
