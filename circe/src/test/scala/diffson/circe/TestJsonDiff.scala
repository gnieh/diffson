package diffson
package test

import circe._
import jsonpatch.test._

class CirceJsonDiff extends TestJsonDiff[io.circe.Json] with TestProtocol
