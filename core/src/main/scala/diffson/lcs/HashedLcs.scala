package diffson.lcs

import HashedLcs._

/** Speeds up LCS computations by pre-computing hashes for all objects.
 *  Very useful for objects that recompute hashCodes on each invocation.
 *
 *  @param delegate Decorated LCS implementation.
 */
class HashedLcs[T](delegate: Lcs[Hashed[T]]) extends Lcs[T] {

  override def lcs(seq1: List[T], seq2: List[T], low1: Int, high1: Int, low2: Int, high2: Int): List[(Int, Int)] = {
    // wrap all values and delegate to proper implementation
    delegate.lcs(seq1.map(x => new Hashed[T](x)), seq2.map(x => new Hashed[T](x)), low1, high1, low2, high2)
  }
}

object HashedLcs {
  /** Wraps provided value together with its hashCode. Equals is overridden to first
   *  check hashCode and then delegate to the wrapped value.
   *
   *  @param value wrapped value
   */
  class Hashed[T](val value: T) {
    override val hashCode: Int = value.hashCode()
    override def equals(other: Any): Boolean = other match {
      case that: Hashed[_] if that.hashCode == hashCode => value.equals(that.value)
      case _ => false
    }
  }
}
