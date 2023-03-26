package diffson.bson4cats

import cats.Eq
import mongo4cats.operations.Update

object test {
  implicit val updateEq: Eq[Update] = Eq.fromUniversalEquals
}
