package probfilter.crdt.immutable

import probfilter.crdt.BaseFilter
import probfilter.pdsa.BloomStrategy

import scala.collection.immutable.BitSet


/** An immutable grow-only replicated bloom filter. */
@SerialVersionUID(1L)
final class GBloomFilter[E] private(val strategy: BloomStrategy[E], val data: BitSet) extends BaseFilter[E, GBloomFilter[E]] {
  def this(strategy: BloomStrategy[E]) = this(strategy, BitSet.empty)

  override def size: Int = {
    val n = data.size
    val m = strategy.numBits
    val k = strategy.numHashes
    if (n < k)
      0
    else if (n == k)
      1
    else if (n < m)
      math.round(-math.log1p(-n.toDouble / m) * m / k).toInt
    else
      math.round(m.toDouble / k).toInt
  }

  override def contains(elem: E): Boolean = {
    strategy.iterator(elem).forall(data.contains)
  }

  override def add(elem: E): GBloomFilter[E] = {
    new GBloomFilter[E](strategy, data concat strategy.iterator(elem))
  }

  override def lteq(that: GBloomFilter[E]): Boolean = {
    this.data.subsetOf(that.data)
  }

  override def merge(that: GBloomFilter[E]): GBloomFilter[E] = {
    new GBloomFilter[E](strategy, that.data union this.data)
  }
}
