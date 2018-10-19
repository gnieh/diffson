package gnieh.diffson
package test
package playJson

import play.api.libs.json._

class PlayTestJsonMergeDiff extends TestJsonMergeDiff[JsValue, PlayJsonInstance](gnieh.diffson.playJson) with TestProtocol
