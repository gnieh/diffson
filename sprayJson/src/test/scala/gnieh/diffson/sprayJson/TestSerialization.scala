package gnieh.diffson
package test
package sprayJson

import spray.json._

class SprayTestSerialization extends TestSerialization[JsValue, SprayJsonInstance](gnieh.diffson.sprayJson) with TestProtocol
