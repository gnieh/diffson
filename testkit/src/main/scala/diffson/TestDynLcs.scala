package diffson.lcs

import cats.implicits._

import org.scalatest._

class TestDynLcs extends TestLcs {

  val lcsImpl = new DynamicProgLcs[Char]

}
