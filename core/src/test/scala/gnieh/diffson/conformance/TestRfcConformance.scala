/*
* This file is part of the diffson project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package gnieh.diffson
package conformance

import org.scalatest._

abstract class TestRfcConformance[JsValue, Instance <: DiffsonInstance[JsValue]](val instance: Instance) extends FunSuite with Matchers {

  import instance._
  import provider._

  type JsArray <: JsValue

  trait ConformanceTest

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

  def load(path: String): List[ConformanceTest]

  def scalatest(t: ConformanceTest) =     t match {
    case SuccessConformanceTest(doc, patch, Some(expected), comment, Some(true)) =>
      ignore(comment.getOrElse(f"$doc patched with $patch")) {
        val p = JsonPatch(patch)
        p(doc) should be(expected)
      }
    case SuccessConformanceTest(doc, patch, Some(expected), comment, _) =>
      test(comment.getOrElse(f"$doc patched with $patch")) {
        val p = JsonPatch(patch)
        p(doc) should be(expected)
      }
    case SuccessConformanceTest(doc, patch, None, comment, Some(true)) =>
      ignore(comment.getOrElse(f"$doc patched with $patch")) {
        val p = JsonPatch(patch)
        p(doc)
      }
    case SuccessConformanceTest(doc, patch, None, comment, _) =>
      test(comment.getOrElse(f"$doc patched with $patch")) {
        val p = JsonPatch(patch)
        p(doc)
      }

    case ErrorConformanceTest(doc, patch, error, comment, Some(true)) =>
      ignore(comment.getOrElse(f"$doc patched with $patch")) {
        val exn = intercept[DiffsonException] {
          val p = JsonPatch(patch)
          p(doc)
        }
        exn.getMessage should be(error)
      }
    case ErrorConformanceTest(doc, patch, error, comment, _) =>
      test(comment.getOrElse(f"$doc patched with $patch")) {
        val exn = intercept[DiffsonException] {
          val p = JsonPatch(patch)
          p(doc)
        }
        exn.getMessage should be(error)
      }

    case CommentConformanceTest(comment) =>
      info(comment)
  }

  info("Specification conformance tests")

  for (t <- load("/conformance/spec_tests.json"))
    scalatest(t)

  info("Misceallaneous tests")

  for (t <- load("/conformance/tests.json"))
    scalatest(t)

}
