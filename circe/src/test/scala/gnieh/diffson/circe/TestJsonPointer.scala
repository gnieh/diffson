package diffson
package test

import circe._
import jsonpointer.test._

class CirceTestJsonPointer extends TestJsonPointer[io.circe.Json] with TestProtocol
