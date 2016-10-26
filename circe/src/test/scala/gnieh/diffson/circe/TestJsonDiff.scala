package gnieh.diffson
package test
package circe

class CirceJsonDiff extends TestJsonDiff[io.circe.Json, CirceInstance](gnieh.diffson.circe) with TestProtocol
