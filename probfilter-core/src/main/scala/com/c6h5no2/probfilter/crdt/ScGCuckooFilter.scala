package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.pdsa.cuckoo.CuckooStrategy
import com.c6h5no2.probfilter.util.ClassEx.Clazz
import com.c6h5no2.probfilter.util.{Immutable => ImmCol, Mutable => MutCol}

import scala.annotation.tailrec


/** A scalable grow-only replicated cuckoo filter. */
sealed trait ScGCuckooFilter[E] extends CvRFilter[E, ScGCuckooFilter[E]] {
  override final def size: Int = state.size

  override final def capacity: Int = state.capacity

  override final def contains(elem: E): Boolean = state.contains(elem)

  @tailrec
  override final def add(elem: E): ScGCuckooFilter[E] = {
    if (contains(elem)) {
      this
    } else {
      val expanded = state.expand()
      val last = expanded.last
      if (last.size < last.capacity) {
        val res = last.tryAdd(elem)
        if (res.isSuccess) {
          val updated = expanded.setLast(res.get)
          return copy(updated)
        }
      }
      copy(expanded.expand(true)).add(elem)
    }
  }

  override def merge(that: ScGCuckooFilter[E]): ScGCuckooFilter[E] = {
    copy(this.state.merge(that.state))
  }

  protected def state: FilterSeries[E, GCuckooFilter[E]]

  protected def copy(state: FilterSeries[E, GCuckooFilter[E]]): ScGCuckooFilter[E]

  override def toString: String = s"${getClass.getShortName}($state)"
}

object ScGCuckooFilter {
  /**
   * @param initStrategy a strategy whose `fpp` is the expected compounded value
   * @note The first sub-filter is created with `strategy.tighten()`;
   *       note the initial `capacity` and `fingerprintBits`.
   */
  def apply[E](mutable: Boolean, initStrategy: CuckooStrategy[E], seed: Int): ScGCuckooFilter[E] = {
    if (mutable)
      new ScGCuckooFilter.Mutable[E](initStrategy, seed)
    else
      new ScGCuckooFilter.Immutable[E](initStrategy, seed)
  }

  @SerialVersionUID(1L)
  final class Immutable[E] private(
    protected val state: FilterSeries[E, GCuckooFilter[E]],
    initStrategy: CuckooStrategy[E],
  ) extends ScGCuckooFilter[E]
    with ImmCol {
    def this(initStrategy: CuckooStrategy[E], seed: Int) = {
      this(new FilterSeries[E, GCuckooFilter[E]](last =>
        new GCuckooFilter.Immutable[E](last.fold(initStrategy)(_.strategy).tighten(), seed)), initStrategy)
    }

    override def fpp: Double = initStrategy.fpp

    override def merge(that: ScGCuckooFilter[E]): ScGCuckooFilter[E] = copy(this.state.fastmerge(that.state))

    override protected def copy(state: FilterSeries[E, GCuckooFilter[E]]): ScGCuckooFilter[E] = {
      new ScGCuckooFilter.Immutable[E](state, this.initStrategy)
    }
  }

  @SerialVersionUID(1L)
  final class Mutable[E] private(
    protected var state: FilterSeries[E, GCuckooFilter[E]],
    initStrategy: CuckooStrategy[E],
  ) extends ScGCuckooFilter[E]
    with MutCol {
    def this(initStrategy: CuckooStrategy[E], seed: Int) = {
      this(new FilterSeries[E, GCuckooFilter[E]](last =>
        new GCuckooFilter.Mutable[E](last.fold(initStrategy)(_.strategy).tighten(), seed)), initStrategy)
    }

    override def fpp: Double = initStrategy.fpp

    override protected def copy(state: FilterSeries[E, GCuckooFilter[E]]): ScGCuckooFilter[E] = {
      this.state = state
      this
    }
  }
}
