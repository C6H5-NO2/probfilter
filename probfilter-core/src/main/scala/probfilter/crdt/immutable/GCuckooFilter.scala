package probfilter.crdt.immutable

import probfilter.crdt.BaseFilter
import probfilter.pdsa.{ByteCuckooTable, CuckooStrategy, MapByteCuckooTable}

import scala.util.control.Breaks.{break, breakable}


/**
 * An immutable grow-only replicated cuckoo filter.
 */
@SerialVersionUID(1L)
final class GCuckooFilter[T] private(val strategy: CuckooStrategy[T], val data: ByteCuckooTable)
  extends BaseFilter[T, GCuckooFilter[T]] {
  def this(strategy: CuckooStrategy[T]) = this(strategy, new MapByteCuckooTable())

  override def mightContains(elem: T): Boolean = {
    val triple = strategy.getCuckooTriple(elem)
    data.at(triple.i).contains(triple.fp) || data.at(triple.j).contains(triple.fp)
  }

  override def add(elem: T): GCuckooFilter[T] = {
    val triple = strategy.getCuckooTriple(elem)

    // check whether the two candidate buckets are saturated
    val sizeAtI = data.at(triple.i).size
    if (sizeAtI > strategy.bucketSize)
      throw new CuckooStrategy.BucketOverflowException(elem, triple.i)
    val sizeAtJ = data.at(triple.j).size
    if (sizeAtJ > strategy.bucketSize)
      throw new CuckooStrategy.BucketOverflowException(elem, triple.j)

    if (mightContains(elem))
      return this

    // if either of the candidate buckets has empty slot
    if (sizeAtI < strategy.bucketSize) {
      val newData = data.at(triple.i).add(triple.fp)
      return new GCuckooFilter[T](this.strategy, newData)
    }
    if (sizeAtJ < strategy.bucketSize) {
      val newData = data.at(triple.j).add(triple.fp)
      return new GCuckooFilter[T](this.strategy, newData)
    }

    // cuckoo displacement
    val iterator = strategy.iterator(triple.i)
    var newData = data
    var swappedFp = triple.fp
    while (iterator.hasNext) {
      val pair = newData.at(iterator.peek).replace(swappedFp)
      newData = pair._1
      swappedFp = pair._2
      val idx = iterator.next(swappedFp)
      val bucket = newData.at(idx)
      val size = bucket.size
      if (size > strategy.bucketSize)
        throw new CuckooStrategy.BucketOverflowException(elem, idx)
      if (size < strategy.bucketSize) {
        newData = bucket.add(swappedFp)
        return new GCuckooFilter[T](this.strategy, newData)
      }
    }

    throw new CuckooStrategy.MaxIterationReachedException(elem)
  }

  override def merge(that: GCuckooFilter[T]): GCuckooFilter[T] = {
    import scala.collection.immutable.{HashMap => ImmuHashMap}
    import scala.collection.mutable.{ArrayBuffer, HashMap => MuHashMap}

    val newData = MuHashMap.empty[Int, ArrayBuffer[Byte]]

    // merge entries
    for (i <- 0 until strategy.numBuckets) {
      this.data.at(i).concat(that.data.at(i)).distinct.foreach { fp =>
        val j = strategy.getAltBucket(fp, i)
        if (j < i && (this.data.at(j).contains(fp) || that.data.at(j).contains(fp))) {
          ;
        } else {
          newData.getOrElseUpdate(i, ArrayBuffer.empty).addOne(fp)
        }
      }
    }

    // (somewhat) rebalance
    for (i <- 0 until strategy.numBuckets) breakable {
      val buffer = newData.getOrElse(i, break())
      val size = buffer.size
      if (size <= strategy.bucketSize)
        break()

      val swappedFps = ArrayBuffer.empty[Byte]
      breakable {
        buffer.foreach { fp =>
          val j = strategy.getAltBucket(fp, i)
          if (j != i) {
            newData.getOrElseUpdate(j, ArrayBuffer.empty).addOne(fp)
            swappedFps.addOne(fp)
            if (size - swappedFps.size <= strategy.bucketSize)
              break()
          }
        }
      }

      buffer.filterInPlace(fp => !swappedFps.contains(fp))
    }

    val immuData = newData.view.mapValues(_.toArray).to(ImmuHashMap)
    new GCuckooFilter[T](this.strategy, new MapByteCuckooTable(immuData))
  }
}
