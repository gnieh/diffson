package gnieh.diffson
package test
package sprayJson

import spray.json._

class SprayTestJsonPointer extends TestJsonPointer[JsValue, SprayJsonInstance](gnieh.diffson.sprayJson) with TestProtocol
