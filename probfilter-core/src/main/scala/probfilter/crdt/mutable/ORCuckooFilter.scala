package probfilter.crdt.mutable

import probfilter.crdt.immutable.VersionVector
import probfilter.pdsa.cuckoo.mutable.{CuckooFilter, TypedCuckooTable}
import probfilter.pdsa.cuckoo.{CuckooStrategy, EntryStorageType, LongVersionedEntry}
import probfilter.util.ArrayOpsEx

import scala.util.Try


/** A mutable observed-remove replicated cuckoo filter. */
@SerialVersionUID(1L)
final class ORCuckooFilter[E] private
(private var state: CuckooFilter[E], private var hist: VersionVector, private val rid: Short)
  extends MutCvFilter[E, ORCuckooFilter[E]] {
  def this(strategy: CuckooStrategy[E], rid: Short) = this(new CuckooFilter[E](strategy), new VersionVector(), rid)

  {
    val storageType = state.strategy.storageType()
    require(
      storageType == EntryStorageType.VERSIONED_LONG,
      s"ORCuckooFilter.<init>: storage type $storageType is not VERSIONED_LONG"
    )
  }

  def strategy: CuckooStrategy[E] = state.strategy

  override def size(): Int = state.size()

  override def capacity(): Int = state.capacity()

  override def fpp(): Double = state.fpp()

  override def contains(elem: E): Boolean = state.contains(elem)

  override def add(elem: E): ORCuckooFilter[E] = {
    val triple = state.strategy.hashAll(elem)
    val entry = LongVersionedEntry.parse(triple.fp, rid, hist.next(rid))
    state.add[Long](triple, entry, elem)
    hist = hist.increase(rid)
    this
  }

  override def tryAdd(elem: E): Try[ORCuckooFilter[E]] = Try.apply(add(elem))

  override def remove(elem: E): ORCuckooFilter[E] = {state.remove(elem); this}

  override def merge(that: ORCuckooFilter[E]): ORCuckooFilter[E] = {
    val thatData = that.state.data.typed[Long]
    val newState = this.state.zipFold(that.state)(TypedCuckooTable.empty[Long]) { (newData, thisBucket, thatBucket, index) =>
      val s124 = thisBucket.filter { entry =>
        if (thatBucket.contains(entry)) {
          true // S1: IN (this.state INTERSECT that.state)
        } else {
          val ce = LongVersionedEntry.create(entry)
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
          val ce = LongVersionedEntry.create(entry)
          !this.hist.observes(ce.replicaId, ce.timestamp) // S3: IN (that.state DIFF this.state) AND NOT IN this.hist
        } else {
          false
        }
      }

      val s = ArrayOpsEx.concated(s124, s3)
      newData.set(index, s)
    }
    val newHist = this.hist.merge(that.hist)
    this.state = newState.rebalance()
    this.hist = newHist
    this
  }

  override def toString: String = s"ORCF($state, $hist, $rid)"
}
