package com.c6h5no2.probfilter.pdsa.bloom

import com.c6h5no2.probfilter.pdsa.Filter
import com.c6h5no2.probfilter.util.{Immutable => ImmCol, Mutable => MutCol}


/** A [[https://doi.org/10.1145/362686.362692 bloom filter]]. */
sealed trait BloomFilter[E] extends Filter[E, BloomFilter[E]] {
  override final def size: Int = {
    val nb = bitset.size
    val m = strategy.numBits
    val k = strategy.numHashes
    if (nb < k)
      0
    else if (nb < m)
      math.round(-m.toDouble / k * math.log1p(-nb.toDouble / m)).toInt
    else
      math.round(m.toDouble / k).toInt
  }

  override final def capacity: Int = strategy.capacity

  override final def fpp: Double = strategy.fpp

  override final def contains(elem: E): Boolean = strategy.hashIterator(elem).forall(bitset.contains)

  override final def add(elem: E): BloomFilter[E] = copy(bitset.add(strategy.hashIterator(elem)))

  final def union(that: BloomFilter[E]): BloomFilter[E] = copy(this.bitset.union(that.bitset))

  def strategy: BloomStrategy[E]

  protected def bitset: BitSet

  protected def copy(bitset: BitSet): BloomFilter[E]

  override def toString: String = s"${getClass.getName}($bitset)"
}

object BloomFilter {
  def apply[E](mutable: Boolean, strategy: BloomStrategy[E]): BloomFilter[E] = {
    if (mutable)
      new BloomFilter.Mutable[E](strategy)
    else
      new BloomFilter.Immutable[E](strategy)
  }

  @SerialVersionUID(1L)
  final class Immutable[E] private(
    protected val bitset: BitSet,
    val strategy: BloomStrategy[E],
  ) extends BloomFilter[E]
    with ImmCol {
    def this(strategy: BloomStrategy[E]) = this(new BitSet.Immutable(), strategy)

    override protected def copy(bitset: BitSet): BloomFilter[E] = {
      new BloomFilter.Immutable[E](bitset, this.strategy)
    }
  }

  @SerialVersionUID(1L)
  final class Mutable[E] private(
    protected val bitset: BitSet,
    val strategy: BloomStrategy[E],
  ) extends BloomFilter[E]
    with MutCol {
    def this(strategy: BloomStrategy[E]) = this(new BitSet.Mutable(), strategy)

    override protected def copy(bitset: BitSet): BloomFilter[E] = this // bitset is mutated in-place
  }
}
