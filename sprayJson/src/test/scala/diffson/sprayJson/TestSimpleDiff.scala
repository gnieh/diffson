package diffson
package test

import sprayJson._
import jsonpatch.test._

class SprayJsonSimpleDiff extends TestSimpleDiff[spray.json.JsValue] with TestProtocol
