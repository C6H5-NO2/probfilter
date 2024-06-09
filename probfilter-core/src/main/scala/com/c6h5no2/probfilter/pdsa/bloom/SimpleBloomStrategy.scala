package com.c6h5no2.probfilter.pdsa.bloom

import com.c6h5no2.probfilter.hash.FoldHash.{BytesHashCode, LongHashCode}
import com.c6h5no2.probfilter.hash.{FarmHashFingerprint64, Funnel, MurMurHash3}


@SerialVersionUID(1L)
final class SimpleBloomStrategy[E] private(
  val capacity: Int,
  val numBits: Int,
  val numHashes: Int,
  desiredFpp: Double,
  funnel: Funnel[_ >: E],
) extends KMBloomStrategy[E] {
  override def tighten(): SimpleBloomStrategy[E] =
    SimpleBloomStrategy.apply(capacity /* + (capacity >>> 1) */ , desiredFpp / 2.0, funnel)

  override protected def hash1(elem: E): Int =
    MurMurHash3.apply(elem, funnel).xorFoldToInt

  override protected def hash2(elem: E): Int =
    FarmHashFingerprint64.apply(elem, funnel).xorFoldToInt
}

object SimpleBloomStrategy {
  /**
   * @param capacity expected number of elements to be inserted
   * @param fpp desired false positive possibility between 0 and 1
   * @param funnel the funnel object to use
   * @throws java.lang.IllegalArgumentException if any argument is illegal
   */
  def apply[E](capacity: Int, fpp: Double, funnel: Funnel[_ >: E]): SimpleBloomStrategy[E] = {
    require(capacity > 0, s"${getClass.getName}.apply: capacity = $capacity not > 0")
    require(0 < fpp && fpp < 1, s"${getClass.getName}.apply: fpp = $fpp not in (0, 1)")
    val m = optimalBits(fpp, capacity)
    require(0 < m && m < Int.MaxValue, s"${getClass.getName}.apply: optimalBits = $m too large")
    val h = optimalHashes(fpp)
    require(0 < h && h < Byte.MaxValue, s"${getClass.getName}.apply: optimalHashes = $h too large")
    new SimpleBloomStrategy[E](capacity, m, h, fpp, funnel)
  }

  private[this] val ln2: Double = math.log(2)

  private def optimalBits(p: Double, n: Int): Int = math.ceil(n * -math.log(p) / (ln2 * ln2)).toInt

  private def optimalHashes(p: Double): Int = math.ceil(-math.log(p) / ln2).toInt
}
