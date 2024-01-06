package probfilter.crdt.immutable

import probfilter.crdt.BaseFilter
import probfilter.pdsa.BloomStrategy

import scala.collection.immutable.BitSet


/**
 * An immutable grow-only replicated bloom filter.
 */
@SerialVersionUID(1L)
final class GBloomFilter[T] private(val strategy: BloomStrategy[T], val data: BitSet)
  extends BaseFilter[T, GBloomFilter[T]] {
  def this(strategy: BloomStrategy[T]) = this(strategy, BitSet.empty)

  override def mightContain(elem: T): Boolean = {
    strategy.iterator(elem).forall(data.contains)
  }

  override def add(elem: T): GBloomFilter[T] = {
    new GBloomFilter[T](strategy, data concat strategy.iterator(elem))
  }

  override def merge(that: GBloomFilter[T]): GBloomFilter[T] = {
    new GBloomFilter[T](strategy, this.data union that.data)
  }
}
