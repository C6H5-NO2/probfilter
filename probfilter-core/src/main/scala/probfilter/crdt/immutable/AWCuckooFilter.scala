package probfilter.crdt.immutable

import probfilter.crdt.{CuckooTable, VectorClock}
import probfilter.pdsa.CuckooStrategy

import scala.util.{Success, Try}


class AWCuckooFilter[T] private(val strategy: CuckooStrategy[T], val clock: VectorClock, val data: CuckooTable)
  extends BaseFilter[T, AWCuckooFilter[T]] {
  def this(strategy: CuckooStrategy[T]) = this(strategy, new VectorClock(), ???)

  override def mightContains(elem: T): Boolean = {
    val pair = strategy.getCuckooPair(elem)
    if (data.atContains(pair.i, pair.fp))
      return true
    val j = strategy.getAltBucket(pair)
    data.atContains(j, pair.fp)
  }

  override def add(elem: T): Try[AWCuckooFilter[T]] = ???

  override def remove(elem: T): Try[AWCuckooFilter[T]] = {
    val pair = strategy.getCuckooPair(elem)
    val j = strategy.getAltBucket(pair)
    val newData = data.atRemove(pair.i, pair.fp).atRemove(j, pair.fp)
    Success(new AWCuckooFilter[T](strategy, clock, newData))
  }

  override def merge(that: AWCuckooFilter[T]): AWCuckooFilter[T] = ???
}
