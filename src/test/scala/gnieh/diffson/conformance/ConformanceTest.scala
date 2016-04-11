package gnieh.diffson
package conformance

import spray.json._
import sprayJson._

sealed trait ConformanceTest

case class SuccessConformanceTest(doc: JsValue,
  patch: JsArray,
  expected: Option[JsValue],
  comment: Option[String],
  disabled: Option[Boolean]) extends ConformanceTest

case class ErrorConformanceTest(doc: JsValue,
  patch: JsArray,
  error: String,
  comment: Option[String],
  disabled: Option[Boolean]) extends ConformanceTest

case class CommentConformanceTest(comment: String) extends ConformanceTest
