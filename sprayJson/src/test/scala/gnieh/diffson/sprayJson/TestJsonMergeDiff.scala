package gnieh.diffson
package test
package sprayJson

import spray.json._

class SprayTestJsonMergeDiff extends TestJsonMergeDiff[JsValue, SprayJsonInstance](gnieh.diffson.sprayJson) with TestProtocol

