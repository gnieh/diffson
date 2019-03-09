package diffson
package test

import sprayJson._
import jsonpatch.test._

class SprayJsonTestArrayDiff extends TestArrayDiff[spray.json.JsValue] with TestProtocol
