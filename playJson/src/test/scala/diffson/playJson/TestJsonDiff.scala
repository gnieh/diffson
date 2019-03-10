package diffson
package playJson

import jsonpatch._

class PlayJsonJsonDiff extends TestJsonDiff[play.api.libs.json.JsValue] with PlayJsonTestProtocol
