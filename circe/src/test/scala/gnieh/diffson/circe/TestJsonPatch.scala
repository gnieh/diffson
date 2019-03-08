package diffson
package test

import circe._
import jsonpatch.test._

class CirceTestJsonPatch extends TestJsonPatch[io.circe.Json] with TestProtocol
