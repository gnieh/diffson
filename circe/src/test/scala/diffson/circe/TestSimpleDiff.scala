package diffson
package test

import circe._
import jsonpatch.test._

class CirceSimpleDiff extends TestSimpleDiff[io.circe.Json] with TestProtocol
