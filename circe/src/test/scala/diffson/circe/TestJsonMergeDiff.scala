package diffson
package circe

import jsonmergepatch._

class CirceJsonMergeDiff extends TestJsonMergeDiff[io.circe.Json] with CirceTestProtocol
