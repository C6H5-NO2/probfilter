package probfilter.crdt.immutable

import probfilter.crdt.BaseFilter
import probfilter.pdsa.{CuckooFilterOps, CuckooStrategy, CuckooTable, LongCuckooEntry}

import scala.collection.immutable.HashMap


/** An immutable observed-remove replicated cuckoo filter. */
@SerialVersionUID(1L)
final class ORCuckooFilter[E] private(val strategy: CuckooStrategy[E], val rid: Short, val version: VersionVector, val data: CuckooTable[Long]) extends BaseFilter[E, ORCuckooFilter[E]] {
  def this(strategy: CuckooStrategy[E], rid: Short) = this(strategy, rid, new VersionVector(), CuckooTable.empty[Long])

  override def size(): Int = data.size

  override def contains(elem: E): Boolean = {
    val triple = strategy.getCuckooTriple(elem)
    val p = (e: Long) => LongCuckooEntry.from(e).fingerprint == triple.fp
    data.at(triple.i).iterator.exists(p) || data.at(triple.j).iterator.exists(p)
  }

  override def add(elem: E): ORCuckooFilter[E] = {
    val triple = strategy.getCuckooTriple(elem)
    val entry = LongCuckooEntry.parse(triple.fp, rid, version.next(rid))
    val newData = CuckooFilterOps.add(triple, entry, data)(strategy, elem)
    new ORCuckooFilter(strategy, rid, version.inc(rid), newData)
  }

  override def remove(elem: E): ORCuckooFilter[E] = {
    val triple = strategy.getCuckooTriple(elem)
    val p = (e: Long) => LongCuckooEntry.from(e).fingerprint == triple.fp
    val ai = data.at(triple.i).iterator.filter(p).toArray
    val aj = data.at(triple.j).iterator.filter(p).toArray
    val len = ai.length + aj.length
    if (len == 0)
      return this
    val rand = CuckooFilterOps.rand(len)
    if (rand < ai.length) {
      val e = ai.apply(rand)
      val newData = data.at(triple.i).remove(e)
      new ORCuckooFilter(strategy, rid, version, newData)
    } else {
      val e = aj.apply(rand - ai.length)
      val newData = data.at(triple.j).remove(e)
      new ORCuckooFilter(strategy, rid, version, newData)
    }
  }

  override def lteq(that: ORCuckooFilter[E]): Boolean = ???

  override def merge(that: ORCuckooFilter[E]): ORCuckooFilter[E] = {
    val buffer = (0 until strategy.numBuckets).foldLeft(HashMap.empty[Int, Array[Long]]) { (buffer, i) =>
      val thisBucket = this.data.at(i).iterator.toArray
      val thatBucket = that.data.at(i).iterator.toArray

      val s124 = thisBucket.iterator.filter { e =>
        if (thatBucket.contains(e)) {
          true // S1
        } else {
          val ce = LongCuckooEntry.from(e)
          if (!that.version.observes(ce.replicaId, ce.timestamp)) {
            true // S2
          } else {
            val j = strategy.getAltBucket(ce.fingerprint, i)
            that.data.at(j).contains(e) // S4
          }
        }
      }

      val s3 = thatBucket.iterator.filter { e =>
        if (!thisBucket.contains(e)) {
          val ce = LongCuckooEntry.from(e)
          !this.version.observes(ce.replicaId, ce.timestamp) // S3
        } else {
          false
        }
      }

      val s = (s124 concat s3).toArray
      if (s.isEmpty) buffer else buffer.updated(i, s)
    }

    import probfilter.pdsa.MapCuckooTable // todo
    val newData = new MapCuckooTable(buffer)
    val newVersion = this.version merge that.version
    new ORCuckooFilter(strategy, rid, newVersion, newData)
  }
}
