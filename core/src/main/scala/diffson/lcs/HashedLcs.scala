/*
 * Copyright 2022 Typelevel
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

package diffson.lcs

import cats._
import cats.implicits._

/** Speeds up LCS computations by pre-computing hashes for all objects.
 *  Very useful for objects that recompute hashCodes on each invocation.
 *
 *  @param delegate Decorated LCS implementation.
 */
class HashedLcs[T: Eq](delegate: Lcs[Hashed[T]]) extends Lcs[T] {

  def savedHashes = this

  override def lcs(seq1: List[T], seq2: List[T], low1: Int, high1: Int, low2: Int, high2: Int): List[(Int, Int)] = {
    // wrap all values and delegate to proper implementation
    delegate.lcs(seq1.map(x => new Hashed[T](x)), seq2.map(x => new Hashed[T](x)), low1, high1, low2, high2)
  }
}

object Hashed {
  implicit def HashesEq[T: Eq]: Eq[Hashed[T]] =
    new Eq[Hashed[T]] {
      override def eqv(h1: Hashed[T], h2: Hashed[T]): Boolean =
        h1.hashCode == h2.hashCode && h1.value === h2.value
    }
}

/** Wraps provided value together with its hashCode. Equals is overridden to first
 *  check hashCode and then delegate to the wrapped value.
 *
 *  @param value wrapped value
 */
class Hashed[T: Eq](val value: T) {
  override val hashCode: Int = value.hashCode()
  override def equals(other: Any): Boolean = other match {
    case that: Hashed[T] @unchecked => this === that
    case _                          => false
  }
}
