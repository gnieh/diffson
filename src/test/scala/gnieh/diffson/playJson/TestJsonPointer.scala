package gnieh.diffson
package test
package playJson

import play.api.libs.json._

class PlayTestJsonPointer extends TestJsonPointer[JsValue, PlayJsonInstance](gnieh.diffson.playJson) with TestProtocol
