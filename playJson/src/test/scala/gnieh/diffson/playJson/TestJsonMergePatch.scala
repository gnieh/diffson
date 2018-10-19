package gnieh.diffson
package test
package playJson

import play.api.libs.json._

class PlayTestJsonMergePatch extends TestJsonMergePatch[JsValue, PlayJsonInstance](gnieh.diffson.playJson) with TestProtocol
