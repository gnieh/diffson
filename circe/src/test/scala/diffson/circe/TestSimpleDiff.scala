package diffson
package circe

import jsonpatch._

class CirceSimpleDiff extends TestSimpleDiff[io.circe.Json] with CirceTestProtocol
