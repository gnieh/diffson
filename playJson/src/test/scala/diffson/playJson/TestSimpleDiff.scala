package diffson
package playJson

import jsonpatch._

class PlayJsonSimpleDiff extends TestSimpleDiff[play.api.libs.json.JsValue] with PlayJsonTestProtocol
