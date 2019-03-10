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
package diffson
package jsonpatch
package conformance

import cats.implicits._

import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Try

abstract class TestRfcConformance[Json: Jsony] extends AnyFunSuite with Matchers {

  trait ConformanceTest

  case class SuccessConformanceTest(
      doc: Json,
      patch: Json,
      expected: Option[Json],
      comment: Option[String],
      disabled: Option[Boolean]) extends ConformanceTest

  case class ErrorConformanceTest(
      doc: Json,
      patch: Json,
      error: String,
      comment: Option[String],
      disabled: Option[Boolean]) extends ConformanceTest

  case class CommentConformanceTest(comment: String) extends ConformanceTest

  def load(path: String): List[ConformanceTest]

  def parsePatch(json: Json): JsonPatch[Json]

  def scalatest(t: ConformanceTest) = t match {
    case SuccessConformanceTest(doc, patch, Some(expected), comment, Some(true)) =>
      ignore(comment.getOrElse(f"$doc patched with $patch")) {
        val p = parsePatch(patch)
        p[Try](doc).get should be(expected)
      }
    case SuccessConformanceTest(doc, patch, Some(expected), comment, _) =>
      test(comment.getOrElse(f"$doc patched with $patch")) {
        val p = parsePatch(patch)
        p[Try](doc).get should be(expected)
      }
    case SuccessConformanceTest(doc, patch, None, comment, Some(true)) =>
      ignore(comment.getOrElse(f"$doc patched with $patch")) {
        val p = parsePatch(patch)
        p[Try](doc).get
      }
    case SuccessConformanceTest(doc, patch, None, comment, _) =>
      test(comment.getOrElse(f"$doc patched with $patch")) {
        val p = parsePatch(patch)
        p[Try](doc).get
      }

    case ErrorConformanceTest(doc, patch, error, comment, Some(true)) =>
      ignore(comment.getOrElse(f"$doc patched with $patch")) {
        val exn = intercept[Exception] {
          val p = parsePatch(patch)
          p[Try](doc).get
        }
        exn.getMessage should be(error)
      }
    case ErrorConformanceTest(doc, patch, error, comment, _) =>
      test(comment.getOrElse(f"$doc patched with $patch")) {
        val exn = intercept[Exception] {
          val p = parsePatch(patch)
          p[Try](doc).get
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
