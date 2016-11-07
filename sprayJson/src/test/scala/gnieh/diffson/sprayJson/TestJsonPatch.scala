package gnieh.diffson
package test
package sprayJson

import spray.json._

class SprayTestJsonPatch extends TestJsonPatch[JsValue, SprayJsonInstance](gnieh.diffson.sprayJson) with TestProtocol
