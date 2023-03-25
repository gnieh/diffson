package diffson

import cats.Eq
import org.bson.conversions.Bson

package object mongoupdate {

  implicit val BsonEq: Eq[Bson] = Eq.fromUniversalEquals

}
