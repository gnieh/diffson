package diffson
package test

import playJson._
import jsonpatch.test._

class PlayJsonJsonDiff extends TestJsonDiff[play.api.libs.json.JsValue] with TestProtocol
