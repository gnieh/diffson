package gnieh.diffson
package test
package playJson

import play.api.libs.json._

class PlayTestArrayDiff extends TestArrayDiff[JsValue, PlayJsonInstance](gnieh.diffson.playJson) with TestProtocol
