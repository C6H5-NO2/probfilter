package probfilter.pdsa.bloom.immutable

import probfilter.pdsa.Filter
import probfilter.pdsa.bloom.{BloomFilterOps, BloomStrategy}

import scala.collection.immutable.BitSet


/** An immutable bloom filter. */
@SerialVersionUID(1L)
final class BloomFilter[E] private(private val data: BitSet, val strategy: BloomStrategy[E]) extends Filter[E] {
  def this(strategy: BloomStrategy[E]) = this(BitSet.empty, strategy)

  override def size(): Int = BloomFilterOps.size(data, strategy)

  override def capacity(): Int = strategy.capacity()

  override def fpp(): Double = strategy.fpp()

  override def contains(elem: E): Boolean = BloomFilterOps.contains(data, strategy, elem)

  override def add(elem: E): BloomFilter[E] = copy(data.concat(strategy.hashIterator(elem)))

  def union(that: BloomFilter[E]): BloomFilter[E] = copy(this.data.union(that.data))

  def subsetOf(that: BloomFilter[E]): Boolean = this.data.subsetOf(that.data)

  private def copy(data: BitSet): BloomFilter[E] = new BloomFilter[E](data, strategy)
}
