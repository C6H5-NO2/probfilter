package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.pdsa.bloom.{BloomFilter, BloomStrategy}
import com.c6h5no2.probfilter.util.ClassEx.Clazz
import com.c6h5no2.probfilter.util.{Immutable => ImmCol, Mutable => MutCol}


/** A grow-only replicated bloom filter. */
sealed trait GBloomFilter[E] extends CvRFilter[E, GBloomFilter[E]] {
  override final def size(): Int = state.size()

  override final def capacity(): Int = state.capacity()

  override final def fpp(): Double = state.fpp()

  override final def contains(elem: E): Boolean = state.contains(elem)

  override final def add(elem: E): GBloomFilter[E] = copy(state.add(elem))

  override final def merge(that: GBloomFilter[E]): GBloomFilter[E] = copy(this.state.union(that.state))

  private[crdt] final def strategy: BloomStrategy[E] = state.strategy

  protected def state: BloomFilter[E]

  protected def copy(state: BloomFilter[E]): GBloomFilter[E]

  override def toString: String = s"${getClass.getShortName}($state)"
}

object GBloomFilter {
  def apply[E](mutable: Boolean, strategy: BloomStrategy[E]): GBloomFilter[E] = {
    if (mutable)
      new GBloomFilter.Mutable[E](strategy)
    else
      new GBloomFilter.Immutable[E](strategy)
  }

  @SerialVersionUID(1L)
  final class Immutable[E] private(
    protected val state: BloomFilter[E],
  ) extends GBloomFilter[E]
    with ImmCol {
    def this(strategy: BloomStrategy[E]) = this(new BloomFilter.Immutable[E](strategy))

    override protected def copy(state: BloomFilter[E]): GBloomFilter[E] = new GBloomFilter.Immutable[E](state)
  }

  @SerialVersionUID(1L)
  final class Mutable[E] private(
    protected val state: BloomFilter[E],
  ) extends GBloomFilter[E]
    with MutCol {
    def this(strategy: BloomStrategy[E]) = this(new BloomFilter.Mutable[E](strategy))

    override protected def copy(state: BloomFilter[E]): GBloomFilter[E] = this // state is mutated in-place
  }
}
