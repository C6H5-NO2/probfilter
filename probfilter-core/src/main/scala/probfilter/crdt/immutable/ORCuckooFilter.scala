package probfilter.crdt.immutable

import probfilter.pdsa.cuckoo._

import scala.util.Try


/** An immutable observed-remove replicated cuckoo filter. */
@SerialVersionUID(1L)
final class ORCuckooFilter[E] private
(val state: CuckooFilter[E], val hist: VersionVector, val rid: Short)
  extends CvFilter[E, ORCuckooFilter[E]] {
  def this(strategy: CuckooStrategy[E], rid: Short) = this(new CuckooFilter[E](strategy), new VersionVector(), rid)

  {
    val storageType = state.strategy.storageType()
    require(storageType == EntryStorageType.LONG, s"ORCuckooFilter.<init>: storage type $storageType is not LONG")
  }

  override def size(): Int = state.size()

  override def capacity(): Int = state.capacity()

  override def fpp(): Double = state.fpp()

  override def contains(elem: E): Boolean = {
    val triple = state.strategy.hashAll(elem)
    val p = (e: Long) => VersionedEntry.extract(e) == triple.fp
    state.data.typed[Long].exists(triple.i, p) || state.data.typed[Long].exists(triple.j, p)
  }

  override def add(elem: E): ORCuckooFilter[E] = {
    val triple = state.strategy.hashAll(elem)
    val entry = VersionedEntry.parse(triple.fp, rid, hist.next(rid))
    val newState = state.add[Long](triple, entry, elem)
    val newHist = hist.increase(rid)
    copy(newState, newHist)
  }

  override def tryAdd(elem: E): Try[ORCuckooFilter[E]] = Try.apply(add(elem))

  override def remove(elem: E): ORCuckooFilter[E] = {
    val newState = state.remove(elem)
    copy(newState)
  }

  override def lteq(that: ORCuckooFilter[E]): Boolean = ???

  override def merge(that: ORCuckooFilter[E]): ORCuckooFilter[E] = {
    val thisData = this.state.data.typed[Long]
    val thatData = that.state.data.typed[Long]
    val newData = thisData.zipFold(thatData)(TypedCuckooTable.empty[Long]) { (newData, thisBucket, thatBucket, index) =>
      val s124 = thisBucket.filter { entry =>
        if (thatBucket.contains(entry)) {
          true // S1: IN (this.state INTERSECT that.state)
        } else {
          val ce = VersionedEntry.create(entry)
          if (!that.hist.observes(ce.replicaId, ce.timestamp)) {
            true // S2: IN (this.state DIFF that.state) AND NOT IN that.hist
          } else {
            val i2 = this.state.strategy.altIndexOf(index, ce.fingerprint)
            thatData.contains(i2, entry) // S4: IN this.state AND displaced() IN that.state
          }
        }
      }

      val s3 = thatBucket.filter { entry =>
        if (!thisBucket.contains(entry)) {
          val ce = VersionedEntry.create(entry)
          !this.hist.observes(ce.replicaId, ce.timestamp) // S3: IN (that.state DIFF this.state) AND NOT IN this.hist
        } else {
          false
        }
      }

      val s = s124.concat(s3)
      newData.set(index, s)
    }

    val newState = state.copy(newData)
    val newHist = this.hist.merge(that.hist)
    copy(newState, newHist)
  }

  def copy(state: CuckooFilter[E]): ORCuckooFilter[E] = new ORCuckooFilter[E](state, hist, rid)

  def copy(hist: VersionVector): ORCuckooFilter[E] = new ORCuckooFilter[E](state, hist, rid)

  def copy(state: CuckooFilter[E], hist: VersionVector): ORCuckooFilter[E] = new ORCuckooFilter[E](state, hist, rid)

  override def toString: String = s"ORCF($state, $hist, $rid)"
}
