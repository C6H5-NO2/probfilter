package probfilter.crdt.immutable

import probfilter.crdt.BaseFilter
import probfilter.crdt.immutable.RWCuckooFilter.RichLongCuckooBucket
import probfilter.pdsa._

import scala.util.control.Breaks.{break, breakable}


/**
 * An immutable remove-wins replicated cuckoo filter.
 */
@SerialVersionUID(1L)
final class RWCuckooFilter[T] private(
  val strategy: CuckooStrategy[T], val sid: Short, val timestamp: Int, val data: LongCuckooTable
) extends BaseFilter[T, RWCuckooFilter[T]] {
  def this(strategy: CuckooStrategy[T], sid: Short) = {
    this(strategy, sid, 0, new MapLongCuckooTable())
  }

  override def mightContain(elem: T): Boolean = {
    val triple = strategy.getCuckooTriple(elem)
    data.at(triple.i).addedContains(triple.fp) || data.at(triple.j).addedContains(triple.fp)
  }

  override def add(elem: T): RWCuckooFilter[T] = {
    val triple = strategy.getCuckooTriple(elem)

    if (data.at(triple.i).addedSize > strategy.bucketSize)
      throw new CuckooStrategy.BucketOverflowException(elem, triple.i)
    if (data.at(triple.j).addedSize > strategy.bucketSize)
      throw new CuckooStrategy.BucketOverflowException(elem, triple.j)

    val mainIdx = if (data.at(triple.j).contains(triple.fp)) triple.j else triple.i
    var newEntries = data.at(mainIdx).asAdded(triple.fp)
    if (newEntries.isEmpty)
      newEntries = Array.apply(CuckooEntry.parse(triple.fp, this.sid, this.timestamp + 1))
    var newData = data.at(mainIdx).remove(triple.fp)

    if (newData.at(mainIdx).size < strategy.bucketSize) {
      newData = newData.at(mainIdx).add(newEntries)
      return new RWCuckooFilter[T](this.strategy, this.sid, this.timestamp + 1, newData)
    }
    val altIdx = strategy.getAltBucket(triple.fp, mainIdx)
    if (newData.at(altIdx).size < strategy.bucketSize) {
      newData = newData.at(altIdx).add(newEntries)
      return new RWCuckooFilter[T](this.strategy, this.sid, this.timestamp + 1, newData)
    }

    val iterator = strategy.iterator(triple.i)
    var swappedEntries = newEntries
    while (iterator.hasNext) {
      val pair = newData.at(iterator.peek).replace(swappedEntries)
      newData = pair._1
      swappedEntries = pair._2
      val fp = CuckooEntry.of(swappedEntries.apply(0)).fingerprint
      val idx = iterator.next(fp)
      val bucket = newData.at(idx)
      val size = bucket.addedSize
      if (size > strategy.bucketSize)
        throw new CuckooStrategy.BucketOverflowException(elem, idx)
      if (size < strategy.bucketSize) {
        newData = bucket.add(swappedEntries)
        return new RWCuckooFilter[T](this.strategy, this.sid, this.timestamp + 1, newData)
      }
    }

    throw new CuckooStrategy.MaxIterationReachedException(elem)
  }

  override def remove(elem: T): RWCuckooFilter[T] = {
    val triple = strategy.getCuckooTriple(elem)
    val newEntry = CuckooEntry.of(triple.fp, this.sid, this.timestamp + 1).withTombstone(true)
    val newData =
      if (data.at(triple.i).contains(triple.fp))
        data.at(triple.i).add(newEntry.toLong)
      else if (data.at(triple.j).contains(triple.fp))
        data.at(triple.j).add(newEntry.toLong)
      else if (data.at(triple.i).size < strategy.bucketSize)
        data.at(triple.i).add(newEntry.toLong)
      else
        data.at(triple.j).add(newEntry.toLong)
    new RWCuckooFilter[T](this.strategy, this.sid, this.timestamp + 1, newData)
  }

  override def merge(that: RWCuckooFilter[T]): RWCuckooFilter[T] = {
    import scala.collection.immutable.{HashMap => ImmuHashMap}
    import scala.collection.mutable.{ArrayBuffer, HashMap => MuHashMap}

    val newData = MuHashMap.empty[Int, ArrayBuffer[Long]]

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

          val buffer = ArrayBuffer.empty[Long]
          (thisEntries concat thatEntries).sortInPlace().foreach { long =>
            val e = CuckooEntry.of(long)
            if (!(e.isTombstone && buffer.contains(e.withTombstone(false).toLong)))
              buffer.addOne(long)
          }

          newData.getOrElseUpdate(i, ArrayBuffer.empty).addAll(buffer)
        }
      }
    }

    for (i <- 0 until strategy.numBuckets) breakable {
      val buffer = newData.getOrElse(i, break())
      val groups = buffer.groupBy(CuckooEntry.of(_).fingerprint)
      val size = groups.size
      if (size <= strategy.bucketSize)
        break()

      val swappedFps = ArrayBuffer.empty[Byte]
      breakable {
        groups.foreachEntry { (fp, ar) =>
          val j = strategy.getAltBucket(fp, i)
          if (j != i) {
            newData.getOrElseUpdate(j, ArrayBuffer.empty).addAll(ar).sortInPlace()
            swappedFps.addOne(fp)
            if (size - swappedFps.size <= strategy.bucketSize)
              break()
          }
        }
      }

      buffer.filterInPlace(long => !swappedFps.contains(CuckooEntry.of(long).fingerprint))
    }

    val immuData = newData.view.mapValues(_.toArray).to(ImmuHashMap)
    val table = new MapLongCuckooTable(immuData)
    new RWCuckooFilter[T](this.strategy, this.sid, this.timestamp, table)
  }
}


object RWCuckooFilter {
  implicit final class RichLongCuckooBucket(private val bucket: LongCuckooBucket) extends AnyVal {
    def addedSize: Int = {
      import scala.collection.mutable.{HashMap => MuHashMap}

      val containedAsAdded = MuHashMap.empty[Byte, Boolean]
      val iterator = bucket.stream().iterator()
      while (iterator.hasNext) {
        val e = CuckooEntry.of(iterator.nextLong())
        val status = containedAsAdded.get(e.fingerprint)
        if (status.isEmpty || status.get)
          containedAsAdded.update(e.fingerprint, !e.isTombstone)
      }
      containedAsAdded.values.count(_ == true)
    }

    def addedContains(fp: Byte): Boolean = {
      val es = bucket.get(fp)
      if (es.isEmpty)
        false
      else
        !es.exists(CuckooEntry.of(_).isTombstone)
    }

    def asAdded(fp: Byte): Array[Long] = {
      bucket.get(fp).map(CuckooEntry.of(_).withTombstone(false).toLong)
    }
  }
}
