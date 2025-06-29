package com.c6h5no2.probfilter.crdt

/**
 * An immutable convergent replicated data type (CvRDT).
 *
 * @tparam T a join semilattice
 */
trait CvRDT[T] {
  /**
   * @return `true` if `this` is partially less than or equal to (lteq) `that` in the join semilattice
   * @note Implementing this operation is optional.
   */
  def lteq(that: T): Boolean = {
    throw new NotImplementedError
  }

  /**
   * @deprecated The naming is not very clear; use [[lteq]] instead.
   */
  @deprecated
  def compare(that: T): Boolean = {
    lteq(that)
  }

  /**
   * @return The least upper bound of `this` and `that`
   */
  def merge(that: T): T
}
