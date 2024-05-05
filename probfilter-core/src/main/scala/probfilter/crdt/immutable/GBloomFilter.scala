package probfilter.crdt.immutable

import probfilter.pdsa.bloom.BloomStrategy
import probfilter.pdsa.bloom.immutable.BloomFilter


/** An immutable grow-only replicated bloom filter. */
@SerialVersionUID(1L)
final class GBloomFilter[E] private(private val state: BloomFilter[E]) extends CvFilter[E, GBloomFilter[E]] {
  def this(strategy: BloomStrategy[E]) = this(new BloomFilter[E](strategy))

  def strategy: BloomStrategy[E] = state.strategy

  override def size(): Int = state.size()

  override def capacity(): Int = state.capacity()

  override def fpp(): Double = state.fpp()

  override def contains(elem: E): Boolean = state.contains(elem)

  override def add(elem: E): GBloomFilter[E] = copy(state.add(elem))

  override def lteq(that: GBloomFilter[E]): Boolean = this.state.subsetOf(that.state)

  override def merge(that: GBloomFilter[E]): GBloomFilter[E] = copy(this.state.union(that.state))

  private def copy(state: BloomFilter[E]): GBloomFilter[E] = new GBloomFilter[E](state)

  override def toString: String = s"GBF($state)"
}
