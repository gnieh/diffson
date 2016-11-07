package gnieh.diffson
package test
package playJson

import play.api.libs.json._

class PlayTestJsonDiff extends TestJsonDiff[JsValue, PlayJsonInstance](gnieh.diffson.playJson) with TestProtocol
