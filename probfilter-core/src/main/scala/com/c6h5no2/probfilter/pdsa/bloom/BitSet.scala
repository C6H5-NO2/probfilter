package com.c6h5no2.probfilter.pdsa.bloom

import com.c6h5no2.probfilter.collection.ArrayMappedTrie
import com.c6h5no2.probfilter.util.{Immutable => ImmCol, Mutable => MutCol}

import scala.collection.immutable.{BitSet => ImmBitSet}
import scala.collection.mutable.{BitSet => MutBitSet}


trait BitSet extends Serializable {
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

  @SerialVersionUID(1L)
  final class Trie private(private val amt: ArrayMappedTrie) extends BitSet {
    def this() = this(new ArrayMappedTrie())

    override def size: Int = amt.iterator.foldLeft(0) { (acc, x) =>
      acc + java.lang.Integer.bitCount(x)
    }

    override def contains(elem: Int): Boolean =
      ((amt.get(elem >>> 5) >>> (elem & 31)) & 1) != 0

    override def add(elems: IterableOnce[Int]): BitSet = {
      val t = elems.iterator.foldLeft(amt)(Trie.add)
      new Trie(t)
    }

    override def union(that: BitSet): BitSet = that match {
      case that: Trie => add(that.amt.iterator)
      case _ => add(that.bitset)
    }

    override protected def bitset: collection.BitSet = throw new UnsupportedOperationException()

    override def toString: String = amt.iterator.mkString("BitMap.Tire(", ", ", ")")
  }

  object Trie {
    private def add(amt: ArrayMappedTrie, i: Int): ArrayMappedTrie = {
      val x0 = amt.get(i >>> 5)
      val x1 = x0 | (1 << (i & 31))
      if (x1 == x0) amt else amt.set(i >>> 5, x1)
    }

    private def remove(amt: ArrayMappedTrie, i: Int): ArrayMappedTrie = {
      val x0 = amt.get(i >>> 5)
      val x1 = x0 & ~(1 << (i & 31))
      if (x1 == x0) amt else amt.set(i >>> 5, x1)
    }
  }
}
