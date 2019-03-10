package diffson.lcs

import cats.implicits._

import org.scalatest._

class TestPatience extends TestLcs {

  val lcsImpl = new Patience[Char]

}
