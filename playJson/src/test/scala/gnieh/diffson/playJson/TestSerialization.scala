package gnieh.diffson
package test
package playJson

import play.api.libs.json._

class PlayTestSerialization extends TestSerialization[JsValue, PlayJsonInstance](gnieh.diffson.playJson) with TestProtocol
