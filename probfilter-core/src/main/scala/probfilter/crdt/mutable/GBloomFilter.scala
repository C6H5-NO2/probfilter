package probfilter.crdt.mutable

import probfilter.pdsa.bloom.BloomStrategy
import probfilter.pdsa.bloom.mutable.BloomFilter


/** A mutable grow-only replicated bloom filter. */
@SerialVersionUID(1L)
final class GBloomFilter[E] private(private val state: BloomFilter[E]) extends MutCvFilter[E, GBloomFilter[E]] {
  def this(strategy: BloomStrategy[E]) = this(new BloomFilter[E](strategy))

  def strategy: BloomStrategy[E] = state.strategy

  override def size(): Int = state.size()

  override def capacity(): Int = state.capacity()

  override def fpp(): Double = state.fpp()

  override def contains(elem: E): Boolean = state.contains(elem)

  override def add(elem: E): GBloomFilter[E] = {state.add(elem); this}

  override def merge(that: GBloomFilter[E]): GBloomFilter[E] = {this.state.union(that.state); this}

  override def toString: String = s"GBF($state)"
}
