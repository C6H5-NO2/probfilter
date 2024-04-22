package probfilter.pdsa.bloom

import probfilter.pdsa.Filter

import scala.collection.immutable.BitSet


@SerialVersionUID(1L)
final class BloomFilter[E] private[bloom](val data: BitSet, val strategy: BloomStrategy[E]) extends Filter[E] {
  def this(strategy: BloomStrategy[E]) = this(BitSet.empty, strategy)

  override def size(): Int = {
    val n = data.size
    val m = strategy.numBits()
    val k = strategy.numHashes()
    if (n < k)
      0
    else if (n < m)
      math.round(-m.toDouble / k * math.log1p(-n.toDouble / m)).toInt
    else
      math.round(m.toDouble / k).toInt
  }

  override def capacity(): Int = strategy.capacity()

  override def fpp(): Double = strategy.fpp()

  override def contains(elem: E): Boolean = strategy.hashIterator(elem).forall(data.contains)

  override def add(elem: E): BloomFilter[E] = copy(data.concat(strategy.hashIterator(elem)))

  def copy(data: BitSet) = new BloomFilter[E](data, strategy)

  override def toString: String = data.toString
}
