package diffson
package test

import sprayJson._
import jsonpatch.test._

class SprayJsonJsonDiff extends TestJsonDiff[spray.json.JsValue] with TestProtocol
