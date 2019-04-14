package diffson
package circe

import jsonpointer._

class CirceTestJsonPointer extends TestJsonPointer[io.circe.Json] with CirceTestProtocol
