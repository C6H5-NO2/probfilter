package com.c6h5no2.probfilter.util


/**
 * A linear congruential generator.
 */
@SerialVersionUID(1L)
class SimpleLCG private(private val state: Int) extends RandomIntGenerator {
  override def nextState(): RandomIntGenerator = {
    val next = (state * 1103515245 + 12345) & Int.MaxValue
    new SimpleLCG(next)
  }

  override def getInt: Int = state
}

object SimpleLCG {
  def apply(seed: Int): SimpleLCG = {
    new SimpleLCG(~seed)
  }
}
