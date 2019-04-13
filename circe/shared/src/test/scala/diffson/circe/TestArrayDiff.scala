package diffson
package circe

import jsonpatch._

class CirceTestArrayDiff extends TestArrayDiff[io.circe.Json] with CirceTestProtocol
