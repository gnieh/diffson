/*
 * Copyright 2024 Diffson Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package diffson

import jsonpatch._
import jsonpointer._
import jsonmergepatch._

import cats._

import scala.util.Try

import scala.language.implicitConversions

trait TestProtocol[Json] {
  implicit def intSeqMarshaller(is: Seq[Int]): Json
  implicit def intSeqUnmarshaller(json: Json): Seq[Int]
  implicit def boolMarshaller(b: Boolean): Json
  implicit def intMarshaller(i: Int): Json
  implicit def stringMarshaller(s: String): Json
  implicit def jsonEq: Eq[Json]

  def parseJson(s: String): Json
  def parsePatch(s: String): JsonPatch[Json]
  def parsePatch(json: Json): JsonPatch[Json]
  def parseMergePatch(s: String): JsonMergePatch[Json]
  def parseMergePatch(json: Json): JsonMergePatch[Json]

  def serializePatch(p: JsonPatch[Json]): Json
  def serializeMergePatch(p: JsonMergePatch[Json]): Json

  def parsePointer(s: String): Pointer =
    Pointer.parse[Try](s).get

}
