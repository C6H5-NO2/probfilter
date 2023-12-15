package probfilter.crdt.immutable

import probfilter.pdsa.BloomStrategy

import scala.collection.immutable
import scala.util.{Success, Try}


@SerialVersionUID(1L)
class GBloomFilter[T] private(val strategy: BloomStrategy[T], val data: immutable.BitSet) extends BaseFilter[T, GBloomFilter[T]] {
  def this(strategy: BloomStrategy[T]) = this(strategy, immutable.BitSet.empty)

  override def mightContains(elem: T): Boolean = {
    strategy.iterator(elem).forall(data.contains)
  }

  override def add(elem: T): Try[GBloomFilter[T]] = {
    Success(new GBloomFilter[T](strategy, data concat strategy.iterator(elem)))
  }

  override def merge(that: GBloomFilter[T]): GBloomFilter[T] = {
    new GBloomFilter[T](strategy, this.data union that.data)
  }
}
