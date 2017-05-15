package gnieh.diffson
package test
package circe

class CirceSimpleDiff extends TestSimpleDiff[io.circe.Json, CirceInstance](gnieh.diffson.circe) with TestProtocol
