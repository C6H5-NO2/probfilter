package probfilter.crdt.immutable

import probfilter.crdt.immutable.AWCuckooFilter.{CuckooBucketOverflowException, CuckooMaxIterationException}
import probfilter.crdt.{CuckooEntry, CuckooTable, VectorClock}
import probfilter.pdsa.CuckooStrategy

import scala.util.{Failure, Success, Try}


class AWCuckooFilter[T] private(
  val strategy: CuckooStrategy[T],
  val sid: Short,
  val clock: VectorClock,
  val data: CuckooTable
) extends BaseFilter[T, AWCuckooFilter[T]] {
  def this(strategy: CuckooStrategy[T], sid: Short) = this(strategy, sid, new VectorClock(), new CuckooTable())

  override def mightContains(elem: T): Boolean = {
    val triple = strategy.getCuckooTriple(elem)
    data.containsAt(triple.fp, triple.i) || data.containsAt(triple.fp, triple.j)
  }

  override def add(elem: T): Try[AWCuckooFilter[T]] = {
    val triple = strategy.getCuckooTriple(elem)

    // check if the initial buckets are overflowing
    if (data.sizeAt(triple.i) > strategy.bucketSize)
      return Failure(new CuckooBucketOverflowException(elem, triple.i))
    if (data.sizeAt(triple.j) > strategy.bucketSize)
      return Failure(new CuckooBucketOverflowException(elem, triple.j))

    var newEntry = new CuckooEntry(triple.fp, this.sid, this.clock.get(this.sid) + 1)
    var newData = data.removeIfAt(_ < newEntry, triple.i).removeIfAt(_ < newEntry, triple.j)

    // if either of the initial buckets has empty slot
    if (newData.sizeAt(triple.i) < strategy.bucketSize) {
      newData = newData.addAt(newEntry, triple.i)
      return Success(new AWCuckooFilter[T](this.strategy, this.sid, this.clock.inc(this.sid), newData))
    }
    if (newData.sizeAt(triple.j) < strategy.bucketSize) {
      newData = data.addAt(newEntry, triple.j)
      return Success(new AWCuckooFilter[T](this.strategy, this.sid, this.clock.inc(this.sid), newData))
    }

    // do cuckoo
    var idx = triple.i
    for (_ <- 0 until strategy.maxIterations) {
      val pair = data.swapAt(newEntry, idx)
      newData = pair._1
      newEntry = pair._2
      idx = strategy.getAltBucket(newEntry.fingerprint, idx)

      val size = data.sizeAt(idx)
      if (size > strategy.bucketSize)
        return Failure(new CuckooBucketOverflowException(elem, idx))
      if (size < strategy.bucketSize) {
        newData = newData.addAt(newEntry, idx)
        return Success(new AWCuckooFilter[T](this.strategy, this.sid, this.clock.inc(this.sid), newData))
      }
    }

    Failure(new CuckooMaxIterationException(elem))
  }

  override def remove(elem: T): AWCuckooFilter[T] = {
    val triple = strategy.getCuckooTriple(elem)
    val newData =
      if (data.containsAt(triple.fp, triple.i)) {
        data.removeOnceIfAt(_.fingerprint == triple.fp, triple.i)
      } else {
        data.removeOnceIfAt(_.fingerprint == triple.fp, triple.j)
      }
    new AWCuckooFilter[T](this.strategy, this.sid, this.clock, newData)
  }

  def removeAll(elem: T): AWCuckooFilter[T] = {
    val triple = strategy.getCuckooTriple(elem)
    val newData =
      data.removeIfAt(_.fingerprint == triple.fp, triple.i).removeIfAt(_.fingerprint == triple.fp, triple.j)
    new AWCuckooFilter[T](this.strategy, this.sid, this.clock, newData)
  }

  override def merge(that: AWCuckooFilter[T]): AWCuckooFilter[T] = ???
}


object AWCuckooFilter {
  class CuckooMaxIterationException(elem: Any)
    extends RuntimeException(s"Reached max number of iterations when trying to add $elem")

  class CuckooBucketOverflowException(elem: Any, i: Int)
    extends RuntimeException(s"Found overflowed bucket at $i when trying to add $elem")
}
