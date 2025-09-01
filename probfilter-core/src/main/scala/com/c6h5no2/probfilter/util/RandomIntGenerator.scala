package com.c6h5no2.probfilter.util


/**
 * An immutable random number generator.
 *
 * @todo Breaking Change! Previous Cuckoo Filters assume mutability of getInt (prev. nextInt).
 */
trait RandomIntGenerator extends Serializable {
  /**
   * @return a <i>new instance</i> of [[RandomIntGenerator]] with the next state
   * @note This operation should be cheap.
   */
  def nextState(): RandomIntGenerator

  /**
   * @return a non-negative random integer
   */
  def getInt: Int

  /**
   * @param bound a positive integer
   * @return a non-negative random integer within `[0, bound)`
   */
  def getInt(bound: Int): Int = {
    getInt % bound
  }
}
