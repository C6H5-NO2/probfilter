package probfilter.pdsa.bloom

import probfilter.hash.{FarmHashFingerprint64, Funnel, MurMurHash3}


@SerialVersionUID(1L)
final class SimpleBloomStrategy[E] private[bloom]
(val capacity: Int, val numBits: Int, val numHashes: Int, private[bloom] val desiredFpp: Double)
(implicit private val funnel: Funnel[_ >: E])
  extends KMBloomStrategy[E] {
  override def tighten(): SimpleBloomStrategy[E] = SimpleBloomStrategy.create(capacity, desiredFpp / 2.0)

  override def hash1(elem: E): Int = MurMurHash3.hash(elem).toInt

  override def hash2(elem: E): Int = FarmHashFingerprint64.hash(elem).toInt
}


object SimpleBloomStrategy {
  /**
   * @param capacity expected number of elements to be inserted
   * @param fpp desired false positive possibility between 0 and 1
   * @param funnel the funnel object to use
   * @throws java.lang.IllegalArgumentException if any argument is illegal
   */
  def create[E](capacity: Int, fpp: Double)(implicit funnel: Funnel[_ >: E]): SimpleBloomStrategy[E] = {
    require(capacity > 0, s"SimpleBloomStrategy.create: capacity = $capacity not > 0")
    require(0 < fpp && fpp < 1, s"SimpleBloomStrategy.create: fpp = $fpp not in (0, 1)")
    val m = optimalBits(fpp, capacity)
    require(0 < m && m < Int.MaxValue, s"SimpleBloomStrategy.create: optimalBits = $m too large")
    val h = optimalHashes(fpp)
    require(0 < h && h < Byte.MaxValue, s"SimpleBloomStrategy.create: optimalHashes = $h too large")
    new SimpleBloomStrategy[E](capacity, m, h, fpp)
  }

  private val ln2: Double = math.log(2)

  private def optimalBits(p: Double, n: Int): Int = math.ceil(n * -math.log(p) / (ln2 * ln2)).toInt

  private def optimalHashes(p: Double): Int = math.ceil(-math.log(p) / ln2).toInt
}
