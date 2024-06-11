package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.pdsa.cuckoo.{CuckooEntryType, CuckooFilter, CuckooStrategy}
import com.c6h5no2.probfilter.util.ClassEx.Clazz
import com.c6h5no2.probfilter.util.{Immutable => ImmCol, Mutable => MutCol}


/** A grow-only replicated cuckoo filter. */
sealed trait GCuckooFilter[E] extends CvRFilter[E, GCuckooFilter[E]] {
  {
    val entryType = state.strategy.entryType
    require(
      entryType.ordinal() <= CuckooEntryType.SIMPLE_SHORT.ordinal(),
      s"${getClass.getShortName}.<init>: entryType $entryType is not CuckooEntryType.SIMPLE_*"
    )
  }

  override final def size: Int = state.size

  override final def capacity: Int = state.capacity

  override final def fpp: Double = state.fpp

  override final def contains(elem: E): Boolean = state.contains(elem)

  override final def add(elem: E): GCuckooFilter[E] = if (contains(elem)) this else copy(state.add(elem))

  override final def merge(that: GCuckooFilter[E]): GCuckooFilter[E] = {
    merge(that, this.state.strategy)
  }

  private def merge[T](that: GCuckooFilter[E], strategy: CuckooStrategy[E]): GCuckooFilter[E] = {
    val newState = this.state.zipFold[T](other = that.state)(this.state) { (newTTable, thisBucket, thatBucket, index) =>
      thatBucket.foldLeft(newTTable) { (newTTable, entry) =>
        if (thisBucket.contains(entry)) {
          newTTable
        } else {
          val altIndex = strategy.altIndexOf(index, strategy.entryType.extractFp(entry))
          if (this.state.contains(altIndex, entry))
            newTTable
          else
            newTTable.add(altIndex, entry) // this.state UNION (that.state DIFF this.state.displaced)
        }
      }
    }
    copy(newState)
  }

  private[crdt] final def strategy: CuckooStrategy[E] = state.strategy

  protected def state: CuckooFilter[E]

  protected def copy(state: CuckooFilter[E]): GCuckooFilter[E]

  override def toString: String = s"${getClass.getShortName}($state)"
}

object GCuckooFilter {
  def apply[E](mutable: Boolean, strategy: CuckooStrategy[E], seed: Int): GCuckooFilter[E] = {
    if (mutable)
      new GCuckooFilter.Mutable[E](strategy, seed)
    else
      new GCuckooFilter.Immutable[E](strategy, seed)
  }

  @SerialVersionUID(1L)
  final class Immutable[E] private(
    protected val state: CuckooFilter[E],
  ) extends GCuckooFilter[E]
    with ImmCol {
    def this(strategy: CuckooStrategy[E], seed: Int) = this(new CuckooFilter.Immutable[E](strategy, seed))

    def this(strategy: CuckooStrategy[E]) = this(strategy, 0)

    override protected def copy(state: CuckooFilter[E]): GCuckooFilter[E] = new GCuckooFilter.Immutable[E](state)
  }

  @SerialVersionUID(1L)
  final class Mutable[E] private(
    protected val state: CuckooFilter[E],
  ) extends GCuckooFilter[E]
    with MutCol {
    def this(strategy: CuckooStrategy[E], seed: Int) = this(new CuckooFilter.Mutable[E](strategy, seed))

    def this(strategy: CuckooStrategy[E]) = this(strategy, 0)

    override protected def copy(state: CuckooFilter[E]): GCuckooFilter[E] = this // state is mutated in-place
  }
}
