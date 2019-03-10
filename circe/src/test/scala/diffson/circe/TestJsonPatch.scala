package diffson
package circe

import jsonpatch._

class CirceTestJsonPatch extends TestJsonPatch[io.circe.Json] with CirceTestProtocol
