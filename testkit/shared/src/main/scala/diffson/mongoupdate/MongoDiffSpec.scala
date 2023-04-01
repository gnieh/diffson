/*
 * Copyright 2022 Lucas Satabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package diffson
package mongoupdate

import cats.Eq
import weaver._

abstract class MongoDiffSpec[Update: Eq, Bson](implicit Updates: Updates[Update, Bson], Jsony: Jsony[Bson])
    extends SimpleIOSuite {

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

  pureTest("prepend to array") {

    val source = doc(Jsony.makeArray(Vector(string("c"), string("d"))))
    val target = doc(Jsony.makeArray(Vector(string("a"), string("b"), string("c"), string("d"))))

    val d = source.diff(target)

    expect.eql(Updates.pushEach(Updates.empty, "value", 0, List(string("a"), string("b"))), d)
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

  pureTest("adding fields") {
    val source = doc(Jsony.makeObject(Map("a" -> int(1), "c" -> int(3))))
    val target = doc(Jsony.makeObject(Map("a" -> int(1), "b" -> int(2), "c" -> int(3), "z" -> int(26))))

    val d = source.diff(target)

    expect.eql(Updates.set(Updates.set(Updates.empty, "value.b", int(2)), "value.z", int(26)), d)
  }

  pureTest("deleting fields") {
    val source = doc(Jsony.makeObject(Map("a" -> int(1), "b" -> int(2), "c" -> int(3), "z" -> int(26))))
    val target = doc(Jsony.makeObject(Map("a" -> int(1), "c" -> int(3))))

    val d = source.diff(target)

    expect.eql(Updates.unset(Updates.unset(Updates.empty, "value.b"), "value.z"), d)
  }

  pureTest("mixed field modifications") {
    val source = doc(
      Jsony.makeObject(
        Map("a" -> int(1), "b" -> int(2), "c" -> int(3), "z" -> Jsony.makeObject(Map("value" -> int(26))))))
    val target = doc(
      Jsony.makeObject(
        Map("a" -> int(1), "c" -> int(3), "d" -> int(4), "z" -> Jsony.makeObject(Map("value" -> int(-1))))))

    val d = source.diff(target)

    expect.eql(
      Updates.set(Updates.set(Updates.unset(Updates.empty, "value.b"), "value.z.value", int(-1)), "value.d", int(4)),
      d)
  }

}
