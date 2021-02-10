package diffson.lcs

import org.scalatest._

class TestDynLcs extends TestLcs {

  val lcsImpl = new DynamicProgLcs[Char]

}
