package diffson
package test

import sprayJson._
import jsonmergepatch.test._

class SprayJsonJsonMergeDiff extends TestJsonMergeDiff[spray.json.JsValue] with TestProtocol
