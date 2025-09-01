package com.c6h5no2.probfilter.util


/**
 * A linear congruential generator.
 */
@SerialVersionUID(1L)
class SimpleLCG(seed: Int) extends RandomIntGenerator {
  private[this] val state = ~seed

  override def nextState(): RandomIntGenerator = {
    val next = (state * 1103515245 + 12345) & Int.MaxValue
    new SimpleLCG(~next) // ~~=0
  }

  override def getInt: Int = state
}
