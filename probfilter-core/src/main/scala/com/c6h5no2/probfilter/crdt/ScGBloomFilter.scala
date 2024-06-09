package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.pdsa.bloom.BloomStrategy
import com.c6h5no2.probfilter.util.ClassEx.Clazz
import com.c6h5no2.probfilter.util.{Immutable => ImmCol, Mutable => MutCol}


/** A scalable grow-only replicated bloom filter. */
sealed trait ScGBloomFilter[E] extends CvRFilter[E, ScGBloomFilter[E]] {
  override final def size: Int = state.size

  override final def capacity: Int = state.capacity

  override final def contains(elem: E): Boolean = state.contains(elem)

  override final def add(elem: E): ScGBloomFilter[E] = {
    if (contains(elem)) {
      this
    } else {
      val expanded = state.expand()
      val updated = expanded.updateLast(_.add(elem))
      copy(updated)
    }
  }

  override def merge(that: ScGBloomFilter[E]): ScGBloomFilter[E] = {
    copy(this.state.merge(that.state))
  }

  protected def state: FilterSeries[E, GBloomFilter[E]]

  protected def copy(state: FilterSeries[E, GBloomFilter[E]]): ScGBloomFilter[E]

  override def toString: String = s"${getClass.getShortName}($state)"
}

object ScGBloomFilter {
  /**
   * @param initStrategy a strategy whose `fpp` is the expected compounded value
   * @note The first sub-filter is created with `strategy.tighten()`; note the initial `capacity`.
   */
  def apply[E](mutable: Boolean, initStrategy: BloomStrategy[E]): ScGBloomFilter[E] = {
    if (mutable)
      new ScGBloomFilter.Mutable[E](initStrategy)
    else
      new ScGBloomFilter.Immutable[E](initStrategy)
  }

  @SerialVersionUID(1L)
  final class Immutable[E] private(
    protected val state: FilterSeries[E, GBloomFilter[E]],
    initStrategy: BloomStrategy[E],
  ) extends ScGBloomFilter[E]
    with ImmCol {
    def this(initStrategy: BloomStrategy[E]) = {
      this(new FilterSeries[E, GBloomFilter[E]](last =>
        new GBloomFilter.Immutable[E](last.fold(initStrategy)(_.strategy).tighten())), initStrategy)
    }

    override def fpp: Double = initStrategy.fpp

    override def merge(that: ScGBloomFilter[E]): ScGBloomFilter[E] = copy(this.state.fastmerge(that.state))

    override protected def copy(state: FilterSeries[E, GBloomFilter[E]]): ScGBloomFilter[E] = {
      new ScGBloomFilter.Immutable[E](state, this.initStrategy)
    }
  }

  @SerialVersionUID(1L)
  final class Mutable[E] private(
    protected var state: FilterSeries[E, GBloomFilter[E]],
    initStrategy: BloomStrategy[E],
  ) extends ScGBloomFilter[E]
    with MutCol {
    def this(initStrategy: BloomStrategy[E]) = {
      this(new FilterSeries[E, GBloomFilter[E]](last =>
        new GBloomFilter.Mutable[E](last.fold(initStrategy)(_.strategy).tighten())), initStrategy)
    }

    override def fpp: Double = initStrategy.fpp

    override protected def copy(state: FilterSeries[E, GBloomFilter[E]]): ScGBloomFilter[E] = {
      this.state = state
      this
    }
  }
}
