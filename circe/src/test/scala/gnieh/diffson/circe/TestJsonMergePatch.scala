package diffson
package test

import circe._
import jsonmergepatch.test._

class CirceTestJsonMergePatch extends TestJsonMergePatch[io.circe.Json] with TestProtocol
