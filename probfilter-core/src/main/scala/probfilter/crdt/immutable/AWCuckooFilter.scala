package probfilter.crdt.immutable

import probfilter.crdt.{BaseFilter, VectorClock}
import probfilter.pdsa.{CuckooEntry, CuckooStrategy, LongCuckooTable, LongMapCuckooTable}
import probfilter.util.UnsignedVal._

import scala.util.control.Breaks.{break, breakable}


/**
 * An immutable add-wins replicated cuckoo filter.
 */
@SerialVersionUID(1L)
final class AWCuckooFilter[T] private(
  val strategy: CuckooStrategy[T], val sid: Short, val clock: VectorClock, val data: LongCuckooTable
) extends BaseFilter[T, AWCuckooFilter[T]] {
  def this(strategy: CuckooStrategy[T], sid: Short) = {
    this(strategy, sid, new VectorClock(), new LongMapCuckooTable())
  }

  override def mightContains(elem: T): Boolean = {
    val triple = strategy.getCuckooTriple(elem)
    data.at(triple.i).contains(triple.fp) || data.at(triple.j).contains(triple.fp)
  }

  override def add(elem: T): AWCuckooFilter[T] = {
    val triple = strategy.getCuckooTriple(elem)

    // check whether the two candidate buckets are saturated
    if (data.at(triple.i).size > strategy.bucketSize)
      throw new AWCuckooFilter.BucketSaturatedException(elem, triple.i)
    if (data.at(triple.j).size > strategy.bucketSize)
      throw new AWCuckooFilter.BucketSaturatedException(elem, triple.j)

    val newEntry = CuckooEntry.of(triple.fp, this.sid, this.clock.get(this.sid) + 1)
    var newData = data.at(triple.i).remove(triple.fp).at(triple.j).remove(triple.fp)

    // if either of the candidate buckets has empty slot
    if (newData.at(triple.i).size < strategy.bucketSize) {
      newData = newData.at(triple.i).add(newEntry.toLong)
      return new AWCuckooFilter[T](this.strategy, this.sid, this.clock.inc(this.sid), newData)
    }
    if (newData.at(triple.j).size < strategy.bucketSize) {
      newData = newData.at(triple.j).add(newEntry.toLong)
      return new AWCuckooFilter[T](this.strategy, this.sid, this.clock.inc(this.sid), newData)
    }

    // cuckoo displacement
    val iterator = strategy.iterator(triple.i)
    var swappedEntries = Array.apply(newEntry.toLong)
    while (iterator.hasNext) {
      val pair = newData.at(iterator.peek).replace(swappedEntries)
      newData = pair._1
      swappedEntries = pair._2
      val fp = CuckooEntry.of(swappedEntries.apply(0)).fingerprint
      val idx = iterator.next(fp)
      val bucket = newData.at(idx)
      val size = bucket.size
      if (size > strategy.bucketSize)
        throw new AWCuckooFilter.BucketSaturatedException(elem, idx)
      if (size < strategy.bucketSize) {
        newData = bucket.add(swappedEntries)
        return new AWCuckooFilter[T](this.strategy, this.sid, this.clock.inc(this.sid), newData)
      }
    }

    throw new AWCuckooFilter.MaxIterationReachedException(elem)
  }

  override def remove(elem: T): AWCuckooFilter[T] = {
    val triple = strategy.getCuckooTriple(elem)
    val newData = data.at(triple.i).remove(triple.fp).at(triple.j).remove(triple.fp)
    new AWCuckooFilter[T](this.strategy, this.sid, this.clock, newData)
  }

  override def merge(that: AWCuckooFilter[T]): AWCuckooFilter[T] = {
    import scala.collection.{immutable, mutable}

    val newData = mutable.HashMap.empty[Int, mutable.ArrayBuffer[Long]]

    // merge entries
    for (i <- 0 until strategy.numBuckets) {
      this.data.at(i).concat(that.data.at(i)).groupBy(CuckooEntry.of(_).fingerprint).foreachEntry { (fp, _) =>
        val j = strategy.getAltBucket(fp, i)
        breakable {
          if (j < i && (this.data.at(j).contains(fp) || that.data.at(j).contains(fp)))
            break()

          val thisEntries =
            if (i != j) this.data.at(i).get(fp) concat this.data.at(j).get(fp) else this.data.at(i).get(fp)
          val thatEntries =
            if (i != j) that.data.at(i).get(fp) concat that.data.at(j).get(fp) else that.data.at(i).get(fp)

          val buffer = mutable.ArrayBuffer.empty[Long]
          buffer.addAll(thisEntries intersect thatEntries)
          buffer.addAll(thisEntries diff thatEntries filter { long =>
            val e = CuckooEntry.of(long)
            e.timestamp gtu that.clock.get(e.replicaId)
          })
          buffer.addAll(thatEntries diff thisEntries filter { long =>
            val e = CuckooEntry.of(long)
            e.timestamp gtu this.clock.get(e.replicaId)
          })

          newData.getOrElseUpdate(i, mutable.ArrayBuffer.empty).addAll(buffer)
        }
      }
    }

    // remove outdated
    newData.mapValuesInPlace { (_, buffer) =>
      buffer.filter(x => !buffer.exists(y => CuckooEntry.of(y) gt CuckooEntry.of(x))).distinct.sortInPlace()
    }

    // (somewhat) rebalance
    for (i <- 0 until strategy.numBuckets) breakable {
      val buffer = newData.getOrElse(i, break())
      val groups = buffer.groupBy(CuckooEntry.of(_).fingerprint)
      val size = groups.size
      if (size <= strategy.bucketSize)
        break()

      val swappedFps = mutable.ArrayBuffer.empty[Byte]
      breakable {
        groups.foreachEntry { (fp, ar) =>
          val j = strategy.getAltBucket(fp, i)
          if (j != i) {
            newData.getOrElseUpdate(j, mutable.ArrayBuffer.empty).addAll(ar).sortInPlace()
            swappedFps.addOne(fp)
            if (size - swappedFps.size <= strategy.bucketSize)
              break()
          }
        }
      }

      buffer.filterInPlace(long => !swappedFps.contains(CuckooEntry.of(long).fingerprint))
    }

    val imData = newData.view.mapValues(_.toArray).to(immutable.HashMap)
    new AWCuckooFilter[T](this.strategy, this.sid, this.clock merge that.clock, new LongMapCuckooTable(imData))
  }
}


object AWCuckooFilter {
  class BucketSaturatedException(elem: Any, i: Int)
    extends RuntimeException(s"Found saturated bucket at $i when trying to add $elem")

  class MaxIterationReachedException(elem: Any)
    extends RuntimeException(s"Reached maximum number of iterations when trying to add $elem")
}
