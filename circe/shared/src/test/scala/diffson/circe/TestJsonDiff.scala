package diffson
package circe

import jsonpatch._

class CirceJsonDiff extends TestJsonDiff[io.circe.Json] with CirceTestProtocol
