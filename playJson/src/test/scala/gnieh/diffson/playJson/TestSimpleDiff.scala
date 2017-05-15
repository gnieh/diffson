package gnieh.diffson
package test
package playJson

import play.api.libs.json._

class PlayTestSimpleDiff extends TestSimpleDiff[JsValue, PlayJsonInstance](gnieh.diffson.playJson) with TestProtocol
