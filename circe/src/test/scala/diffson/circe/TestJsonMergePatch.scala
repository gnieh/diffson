package diffson
package circe

import jsonmergepatch._

class CirceTestJsonMergePatch extends TestJsonMergePatch[io.circe.Json] with CirceTestProtocol
