package com.c6h5no2.probfilter.pdsa.bloom

import scala.collection.AbstractIterator


/** [[https://doi.org/10.1007/11841036_42 Kirsch-Mitzenmacher optimization]]. */
trait KMBloomStrategy[E] extends BloomStrategy[E] {
  override final def hashIterator(elem: E): Iterator[Int] = {
    val h1 = hash1(elem)
    val h2 = hash2(elem)
    new KMBloomStrategy.KMBloomIterator(h1, h2, this)
  }

  /** @note No need to clamp. */
  protected def hash1(elem: E): Int

  /** @note No need to clamp. */
  protected def hash2(elem: E): Int
}

object KMBloomStrategy {
  private final class KMBloomIterator private(
    private[this] var combined: Int,
    hash2: Int,
    numBits: Int,
    numHashes: Int,
  ) extends AbstractIterator[Int] {
    def this(hash1: Int, hash2: Int, strategy: BloomStrategy[_]) =
      this(hash1, hash2, strategy.numBits, strategy.numHashes)

    private[this] var iter: Int = 0

    override def hasNext: Boolean = iter < numHashes

    override def next(): Int = {
      iter += 1
      val result = (combined & Int.MaxValue) % numBits
      combined += hash2
      result
    }
  }
}
