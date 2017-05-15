package gnieh.diffson
package test
package sprayJson

import spray.json._

class SprayTestSimpleDiff extends TestSimpleDiff[JsValue, SprayJsonInstance](gnieh.diffson.sprayJson) with TestProtocol
