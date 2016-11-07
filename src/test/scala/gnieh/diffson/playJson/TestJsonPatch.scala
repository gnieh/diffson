package gnieh.diffson
package test
package playJson

import play.api.libs.json._

class PlayTestJsonPatch extends TestJsonPatch[JsValue, PlayJsonInstance](gnieh.diffson.playJson) with TestProtocol
