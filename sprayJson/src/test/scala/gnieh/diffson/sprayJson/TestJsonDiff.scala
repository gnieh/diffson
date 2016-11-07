package gnieh.diffson
package test
package sprayJson

import spray.json._

class SprayTestJsonDiff extends TestJsonDiff[JsValue, SprayJsonInstance](gnieh.diffson.sprayJson) with TestProtocol

