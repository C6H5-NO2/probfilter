package probfilter.crdt.immutable

import probfilter.pdsa.bloom.{BloomFilter, BloomStrategy}


/** An immutable grow-only replicated bloom filter. */
@SerialVersionUID(1L)
final class GBloomFilter[E] private(val state: BloomFilter[E]) extends CvFilter[E, GBloomFilter[E]] {
  def this(strategy: BloomStrategy[E]) = this(new BloomFilter[E](strategy))

  override def size(): Int = state.size()

  override def capacity(): Int = state.capacity()

  override def fpp(): Double = state.fpp()

  override def contains(elem: E): Boolean = state.contains(elem)

  override def add(elem: E): GBloomFilter[E] = copy(state.add(elem))

  override def lteq(that: GBloomFilter[E]): Boolean = this.state.data.subsetOf(that.state.data)

  override def merge(that: GBloomFilter[E]): GBloomFilter[E] = copy(state.copy(this.state.data.union(that.state.data)))

  def copy(state: BloomFilter[E]): GBloomFilter[E] = new GBloomFilter[E](state)

  override def toString: String = s"GBF($state)"
}
