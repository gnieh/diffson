package gnieh.diffson
package test
package sprayJson

import spray.json._

class SprayTestJsonMergePatch extends TestJsonMergePatch[JsValue, SprayJsonInstance](gnieh.diffson.sprayJson) with TestProtocol
