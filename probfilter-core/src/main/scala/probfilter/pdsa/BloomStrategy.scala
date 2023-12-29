package probfilter.pdsa

import probfilter.hash.{Funnel, MurmurHash3}
import probfilter.util.JavaFriendly

import java.util.{Iterator => JavaIterator}
import scala.collection.AbstractIterator


@SerialVersionUID(1L)
class BloomStrategy[T] private(val numBits: Int, val numHashes: Int)(implicit val funnel: Funnel[_ >: T])
  extends Serializable {
  /**
   * Returns an iterator over the indices corresponding to `elem`.
   */
  def iterator(elem: T): Iterator[Int] = {
    val hash = MurmurHash3.hash(elem)
    val hash1 = hash.toInt
    val hash2 = (hash >>> 32).toInt
    new AbstractIterator[Int] {
      private var combined = hash1
      private var i = 0

      override def hasNext: Boolean = i < numHashes

      override def next(): Int = {
        val result = (combined & Int.MaxValue) % numBits
        combined += hash2
        i += 1
        result
      }
    }
  }

  @JavaFriendly(scalaDelegate = "iterator")
  def iteratorAsJava(elem: T): JavaIterator[Integer] = new JavaIterator[Integer] {
    private val scalaIterator = iterator(elem)

    override def hasNext: Boolean = scalaIterator.hasNext

    override def next(): Integer = scalaIterator.next()
  }
}


object BloomStrategy {
  /**
   * @param capacity expected number of elements to be inserted
   * @param fpp false positive possibility between 0 and 1
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
    new BloomStrategy[T](m, h)
  }

  private val ln2: Double = math.log(2)

  private def optimalBits(p: Double, n: Int): Int = math.ceil(n * -math.log(p) / (ln2 * ln2)).toInt

  private def optimalHashes(p: Double): Int = math.ceil(-math.log(p) / ln2).toInt
}
