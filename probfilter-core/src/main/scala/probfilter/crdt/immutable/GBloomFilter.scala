package probfilter.crdt.immutable

import probfilter.crdt.BaseFilter
import probfilter.pdsa.BloomStrategy
import probfilter.util.Mergeable

import scala.collection.immutable


/**
 * An immutable grow-only replicated bloom filter.
 */
@SerialVersionUID(1L)
class GBloomFilter[T] private(val strategy: BloomStrategy[T], val data: immutable.BitSet)
  extends BaseFilter[T, GBloomFilter[T]] with Mergeable[GBloomFilter[T]] {
  def this(strategy: BloomStrategy[T]) = this(strategy, immutable.BitSet.empty)

  override def mightContains(elem: T): Boolean = {
    strategy.iterator(elem).forall(data.contains)
  }

  override def add(elem: T): GBloomFilter[T] = {
    new GBloomFilter[T](strategy, data concat strategy.iterator(elem))
  }

  override def merge(that: GBloomFilter[T]): GBloomFilter[T] = {
    new GBloomFilter[T](strategy, this.data union that.data)
  }
}
