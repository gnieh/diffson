package diffson.lcs
package test

import cats.implicits._

import org.scalatest._

class TestDynLcs extends TestLcs {

  val lcsImpl = new DynamicProgLcs[Char]

}
