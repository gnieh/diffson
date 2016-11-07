package gnieh.diffson
package test
package sprayJson

import spray.json._

class SprayTestArrayDiff extends TestArrayDiff[JsValue, SprayJsonInstance](gnieh.diffson.sprayJson) with TestProtocol
