package diffson
package test

import circe._
import jsonpatch.test._

class CirceTestArrayDiff extends TestArrayDiff[io.circe.Json] with TestProtocol
