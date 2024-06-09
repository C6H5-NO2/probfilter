package probfilter.pdsa.bloom.mutable

import probfilter.pdsa.Filter
import probfilter.pdsa.bloom.{BloomFilterOps, BloomStrategy}

import scala.collection.mutable


/** A mutable bloom filter. */
@SerialVersionUID(1L)
final class BloomFilter[E] private(private val data: mutable.BitSet, val strategy: BloomStrategy[E]) extends Filter[E] {
  def this(strategy: BloomStrategy[E]) = this(mutable.BitSet.empty, strategy)

  override def size(): Int = BloomFilterOps.size(data, strategy)

  override def capacity(): Int = strategy.capacity()

  override def fpp(): Double = strategy.fpp()

  override def contains(elem: E): Boolean = BloomFilterOps.contains(data, strategy, elem)

  override def add(elem: E): BloomFilter[E] = {data.++=(strategy.hashIterator(elem)); this}

  def union(that: BloomFilter[E]): BloomFilter[E] = {this.data.|=(that.data); this}
}
