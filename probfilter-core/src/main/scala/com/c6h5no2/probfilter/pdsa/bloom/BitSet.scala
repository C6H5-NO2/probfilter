package com.c6h5no2.probfilter.pdsa.bloom

import com.c6h5no2.probfilter.util.{Immutable => ImmCol, Mutable => MutCol}

import scala.collection.immutable.{BitSet => ImmBitSet}
import scala.collection.mutable.{BitSet => MutBitSet}


sealed trait BitSet extends Serializable {
  def size: Int

  def contains(elem: Int): Boolean

  def add(elems: IterableOnce[Int]): BitSet

  def union(that: BitSet): BitSet

  protected def bitset: collection.BitSet
}

object BitSet {
  def apply(mutable: Boolean): BitSet = {
    if (mutable)
      new BitSet.Mutable()
    else
      new BitSet.Immutable()
  }

  @SerialVersionUID(1L)
  final class Immutable private(protected val bitset: ImmBitSet) extends BitSet with ImmCol {
    def this() = this(ImmBitSet.empty)

    override def size: Int = bitset.size

    override def contains(elem: Int): Boolean = bitset.contains(elem)

    override def add(elems: IterableOnce[Int]): BitSet = copy(bitset.++(elems))

    override def union(that: BitSet): BitSet = copy(this.bitset.|(that.bitset))

    private def copy(bitset: ImmBitSet): BitSet = new BitSet.Immutable(bitset)
  }

  @SerialVersionUID(1L)
  final class Mutable private(protected val bitset: MutBitSet) extends BitSet with MutCol {
    def this() = this(MutBitSet.empty)

    override def size: Int = bitset.size

    override def contains(elem: Int): Boolean = bitset.contains(elem)

    override def add(elems: IterableOnce[Int]): BitSet = copy(bitset.++=(elems))

    override def union(that: BitSet): BitSet = copy(this.bitset.|=(that.bitset))

    private def copy(bitset: MutBitSet): BitSet = this // bitset is mutated in-place
  }
}
