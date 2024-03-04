package probfilter.pdsa

import probfilter.hash.{FarmHashFingerprint64, Funnel, MurMurHash3}

import scala.collection.AbstractIterator


@SerialVersionUID(1L)
class BloomStrategy[T] private(val numBits: Int, val numHashes: Int, val capacity: Int, val desiredFpp: Double)
                              (implicit val funnel: Funnel[_ >: T]) extends Serializable {
  val fpp: Double = math.pow(-math.expm1(-1.0 * numHashes * capacity / numBits), numHashes)

  /** Returns an iterator over the indices corresponding to `elem`. */
  def iterator(elem: T): Iterator[Int] = {
    val hash1 = MurMurHash3.hash(elem).toInt
    val hash2 = FarmHashFingerprint64.hash(elem).toInt
    new BloomStrategy.BloomIterator(hash1, hash2, this)
  }
}


object BloomStrategy {
  /**
   * @param capacity expected number of elements to be inserted
   * @param fpp desired false positive possibility between 0 and 1
   * @param funnel the funnel object to use
   * @throws IllegalArgumentException if any parameter is out of range
   */
  def create[T](capacity: Int, fpp: Double)(implicit funnel: Funnel[_ >: T]): BloomStrategy[T] = {
    require(capacity > 0, s"BloomStrategy.create: capacity = $capacity not > 0")
    require(0 < fpp && fpp < 1, s"BloomStrategy.create: fpp = $fpp not in (0, 1)")
    val m = optimalBits(fpp, capacity)
    require(0 < m && m < Int.MaxValue, s"BloomStrategy.create: optimalBits = $m too large")
    val h = optimalHashes(fpp)
    require(0 < h && h < Byte.MaxValue, s"BloomStrategy.create: optimalHashes = $h too large")
    new BloomStrategy[T](m, h, capacity, fpp)
  }

  private val ln2: Double = math.log(2)

  private def optimalBits(p: Double, n: Int): Int = math.ceil(n * -math.log(p) / (ln2 * ln2)).toInt

  private def optimalHashes(p: Double): Int = math.ceil(-math.log(p) / ln2).toInt

  private final class BloomIterator private
  (private var combined: Int, private val hash2: Int, private val numBits: Int, private val numHashes: Int)
    extends AbstractIterator[Int] {
    def this(hash1: Int, hash2: Int, strategy: BloomStrategy[_]) = this(hash1, hash2, strategy.numBits, strategy.numHashes)

    private var iter: Int = 0

    override def hasNext: Boolean = iter < numHashes

    override def next(): Int = {
      iter += 1
      val result = (combined & Int.MaxValue) % numBits
      combined += hash2
      result
    }
  }
}
