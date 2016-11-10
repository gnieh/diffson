package gnieh.diffson
package test
package circe

class CirceTestJsonPatch extends TestJsonPatch[io.circe.Json, CirceInstance](gnieh.diffson.circe) with TestProtocol
