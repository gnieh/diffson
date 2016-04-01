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

import scala.collection.generic.FilterMonadic

object collection {

  class WithFilter(p: Operation => Boolean, patch: JsonPatch) {

    def map(f: Operation => Operation): JsonPatch =
      patch.flatMap(op => if (p(op)) JsonPatch(f(op)) else EmptyPatch)

    def flatMap(f: Operation => JsonPatch): JsonPatch =
      patch.flatMap(op => if (p(op)) f(op) else EmptyPatch)

    def withFilter(p2: Operation => Boolean): WithFilter =
      new WithFilter(op => p(op) && p2(op), patch)

    def foreach(f: Operation => Unit): Unit =
      patch.foreach(op => if (p(op)) f(op))

  }

  implicit class CollectionOps(val patch: JsonPatch) extends AnyVal {

    def map(f: Operation => Operation): JsonPatch =
      JsonPatch(patch.ops.map(f))

    def flatMap(f: Operation => JsonPatch): JsonPatch =
      JsonPatch(for {
        op <- patch.ops
        JsonPatch(ops) = f(op)
        op <- ops
      } yield op)

    def filter(p: Operation => Boolean): JsonPatch =
      JsonPatch(patch.ops.filter(p))

    def withFilter(p: Operation => Boolean): WithFilter =
      new WithFilter(p, patch)

    def foldLeft[Res](zero: Res)(f: (Res, Operation) => Res): Res =
      patch.ops.foldLeft(zero)(f)

    def foldRight[Res](zero: Res)(f: (Operation, Res) => Res): Res =
      patch.ops.foldRight(zero)(f)

    def foreach(f: Operation => Unit): Unit =
      patch.ops.foreach(f)

    def collect[T](pf: PartialFunction[Operation, T]): Seq[T] =
      patch.ops.collect(pf)
  }
}
