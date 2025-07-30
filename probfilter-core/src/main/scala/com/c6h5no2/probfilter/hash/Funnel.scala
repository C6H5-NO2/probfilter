package com.c6h5no2.probfilter.hash


/**
 * A funnel that unpacks an object as primitive values.
 *
 * @see Funnels
 */
trait Funnel[T] extends Serializable {
  /**
   * Unpacks `T` `from` as primitives into `Sink` `into`.
   */
  def apply(from: T, into: Sink): Unit
}
