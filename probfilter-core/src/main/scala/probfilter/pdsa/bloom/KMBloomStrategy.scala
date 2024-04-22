package probfilter.pdsa.bloom

import scala.collection.AbstractIterator


/** @note Kirsch-Mitzenmacher optimization */
trait KMBloomStrategy[E] extends BloomStrategy[E] {
  override def hashIterator(elem: E): Iterator[Int] = {
    val h1 = hash1(elem)
    val h2 = hash2(elem)
    new KMBloomStrategy.KMBloomIterator(h1, h2, this)
  }

  /** @note No need to clamp. */
  def hash1(elem: E): Int

  /** @note No need to clamp. */
  def hash2(elem: E): Int
}


object KMBloomStrategy {
  private final class KMBloomIterator private
  (private var combined: Int, private val hash2: Int, private val numBits: Int, private val numHashes: Int)
    extends AbstractIterator[Int] {
    def this(hash1: Int, hash2: Int, strategy: BloomStrategy[_]) =
      this(hash1, hash2, strategy.numBits, strategy.numHashes)

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
