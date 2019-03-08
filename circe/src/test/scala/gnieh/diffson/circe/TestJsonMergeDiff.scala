package diffson
package test

import circe._
import jsonmergepatch.test._

class CirceJsonMergeDiff extends TestJsonMergeDiff[io.circe.Json] with TestProtocol
