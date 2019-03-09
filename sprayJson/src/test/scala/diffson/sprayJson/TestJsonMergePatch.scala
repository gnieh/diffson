package diffson
package test

import sprayJson._
import jsonmergepatch.test._

class SprayJsonTestJsonMergePatch extends TestJsonMergePatch[spray.json.JsValue] with TestProtocol
