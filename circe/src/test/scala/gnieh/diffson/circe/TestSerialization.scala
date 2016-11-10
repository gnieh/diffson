package gnieh.diffson
package test
package circe

import io.circe._

class CirceTestSerialization extends TestSerialization[Json, CirceInstance](gnieh.diffson.circe) with TestProtocol
