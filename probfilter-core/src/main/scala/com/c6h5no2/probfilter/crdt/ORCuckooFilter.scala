package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.pdsa.cuckoo._
import com.c6h5no2.probfilter.util.{ArrayOpsEx, Immutable => ImmCol, Mutable => MutCol}


/** An observed-remove replicated cuckoo filter. */
sealed trait ORCuckooFilter[E] extends CvRFilter[E, ORCuckooFilter[E]] {
  {
    val entryType = state.strategy.entryType
    require(
      entryType.ordinal() >= CuckooEntryType.VERSIONED_INT.ordinal(),
      s"${getClass.getName}.<init>: entryType $entryType is not CuckooEntryType.VERSIONED_*"
    )
  }

  override final def size: Int = state.size

  override final def capacity: Int = state.capacity

  override final def fpp: Double = state.fpp

  override final def contains(elem: E): Boolean = state.contains(elem)

  override final def add(elem: E): ORCuckooFilter[E] = {
    val newState = (state.strategy.entryType: @unchecked) match {
      case CuckooEntryType.VERSIONED_INT =>
        state.add[Int](elem, fp => IntVersionedEntry.parse(fp, rid, hist.next(rid)))
      case CuckooEntryType.VERSIONED_LONG =>
        state.add[Long](elem, fp => LongVersionedEntry.parse(fp, rid, hist.next(rid)))
    }
    val newHist = hist.increase(rid)
    copy(newState, newHist)
  }

  override final def remove(elem: E): ORCuckooFilter[E] = copy(state.remove(elem))

  override final def merge(that: ORCuckooFilter[E]): ORCuckooFilter[E] = {
    (this.state.strategy.entryType: @unchecked) match {
      case CuckooEntryType.VERSIONED_INT =>
        merge[Int](that, this.state.strategy, data => ORCuckooFilter.VersionedEntry(IntVersionedEntry(data)))
      case CuckooEntryType.VERSIONED_LONG =>
        merge[Long](that, this.state.strategy, data => ORCuckooFilter.VersionedEntry(LongVersionedEntry(data)))
    }
  }

  private final def merge[T](
    that: ORCuckooFilter[E],
    strategy: CuckooStrategy[E],
    entryParser: T => ORCuckooFilter.VersionedEntry,
  ): ORCuckooFilter[E] = {
    val newState = this.state.zipFold[T](that.state) { (newTTable, thisBucket, thatBucket, index) =>
      val s124 = thisBucket.filter { entry =>
        if (thatBucket.contains(entry)) {
          true // S1: IN (this.state INTERSECT that.state)
        } else {
          val ce = entryParser.apply(entry)
          if (!that.hist.observes(ce.replicaId, ce.timestamp)) {
            true // S2: IN (this.state DIFF that.state) AND NOT IN that.hist
          } else {
            val altIndex = strategy.altIndexOf(index, ce.fingerprint)
            that.state.contains(altIndex, entry) // S4: IN this.state AND (displaced() IN that.state)
          }
        }
      }

      val s3 = thatBucket.filter { entry =>
        if (!thisBucket.contains(entry)) {
          val ce = entryParser.apply(entry)
          // S3: IN (that.state DIFF this.state) AND NOT IN this.hist
          !this.hist.observes(ce.replicaId, ce.timestamp)
        } else {
          false
        }
      }

      val s = ArrayOpsEx.concated(s124, s3)
      newTTable.set(index, s)
    }
    val newHist = this.hist.merge(that.hist)
    copy(newState, newHist)
  }

  private[crdt] final def strategy: CuckooStrategy[E] = state.strategy

  private[crdt] def hist: VersionVector

  protected def rid: Short

  protected def state: CuckooFilter[E]

  private[crdt] def copy(hist: VersionVector): ORCuckooFilter[E]

  protected def copy(state: CuckooFilter[E]): ORCuckooFilter[E]

  protected def copy(state: CuckooFilter[E], hist: VersionVector): ORCuckooFilter[E]

  override def toString: String = s"${getClass.getName}($rid, $state, $hist)"
}

object ORCuckooFilter {
  def apply[E](mutable: Boolean, strategy: CuckooStrategy[E], rid: Short, seed: Int): ORCuckooFilter[E] = {
    if (mutable)
      new ORCuckooFilter.Mutable[E](strategy, rid, seed)
    else
      new ORCuckooFilter.Immutable[E](strategy, rid, seed)
  }

  @SerialVersionUID(1L)
  final class Immutable[E] private(
    protected val state: CuckooFilter[E],
    private[crdt] val hist: VersionVector,
    protected val rid: Short,
  ) extends ORCuckooFilter[E]
    with ImmCol {
    def this(strategy: CuckooStrategy[E], rid: Short, seed: Int) =
      this(new CuckooFilter.Immutable[E](strategy, seed), new VersionVector(), rid)

    def this(strategy: CuckooStrategy[E], rid: Short) = this(strategy, rid, rid)

    override private[crdt] def copy(hist: VersionVector): ORCuckooFilter[E] = {
      new ORCuckooFilter.Immutable[E](this.state, hist, this.rid)
    }

    override protected def copy(state: CuckooFilter[E]): ORCuckooFilter[E] = {
      new ORCuckooFilter.Immutable[E](state, this.hist, this.rid)
    }

    override protected def copy(state: CuckooFilter[E], hist: VersionVector): ORCuckooFilter[E] = {
      new ORCuckooFilter.Immutable[E](state, hist, this.rid)
    }
  }

  @SerialVersionUID(1L)
  final class Mutable[E] private(
    protected val state: CuckooFilter[E],
    private[this] var _hist: VersionVector,
    protected val rid: Short,
  ) extends ORCuckooFilter[E]
    with MutCol {
    def this(strategy: CuckooStrategy[E], rid: Short, seed: Int) =
      this(new CuckooFilter.Mutable[E](strategy, seed), new VersionVector(), rid)

    def this(strategy: CuckooStrategy[E], rid: Short) = this(strategy, rid, rid)

    override private[crdt] def hist: VersionVector = _hist

    override private[crdt] def copy(hist: VersionVector): ORCuckooFilter[E] = {
      this._hist = hist
      this
    }

    override protected def copy(state: CuckooFilter[E]): ORCuckooFilter[E] = {
      // state is mutated in-place
      this
    }

    override protected def copy(state: CuckooFilter[E], hist: VersionVector): ORCuckooFilter[E] = {
      // state is mutated in-place
      this._hist = hist
      this
    }
  }

  private final class VersionedEntry private(val fingerprint: Short, val replicaId: Short, val timestamp: Int)

  private object VersionedEntry {
    @inline def apply(entry: IntVersionedEntry): VersionedEntry =
      new VersionedEntry(entry.fingerprint, entry.replicaId, entry.timestamp)

    @inline def apply(entry: LongVersionedEntry): VersionedEntry =
      new VersionedEntry(entry.fingerprint, entry.replicaId, entry.timestamp)
  }
}
