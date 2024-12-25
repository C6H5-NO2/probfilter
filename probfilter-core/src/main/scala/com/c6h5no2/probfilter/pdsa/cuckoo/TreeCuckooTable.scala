package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.collection.ArrayMappedTrie
import com.c6h5no2.probfilter.pdsa.cuckoo.TreeCuckooTable.ArrayMappedTrieEx
import com.c6h5no2.probfilter.util.ArrayOpsEx
import com.c6h5no2.probfilter.util.ClassEx.Clazz

import scala.collection.AbstractIterator
import scala.reflect.{ClassTag, classTag}


/**
 * A tree-based solution for immutable cuckoo table in upcoming versions.
 */
@SerialVersionUID(1L)
final class TreeCuckooTable[T: ClassTag] private(
  data: ArrayMappedTrie,
  overflowed: MapCuckooTable[T],
  val numBuckets: Int,
  val bucketSize: Int,
  val size: Int,
) extends TypedCuckooTable[T] {
  def this(numBuckets: Int, bucketSize: Int) = this(
    new ArrayMappedTrie(),
    new MapCuckooTable.Immutable[T](bucketSize),
    numBuckets,
    bucketSize,
    0
  )

  override def storageType: ClassTag[T] = classTag[T]

  override def get(index: Int): Array[T] = {
    val from = index * bucketSize
    val d = data.slice(from, from + bucketSize).filter(_ != 0L.asInstanceOf[T]).toArray[T]
    val o = overflowed.get(index)
    ArrayOpsEx.concated(d, o)
  }

  override def set(index: Int, value: Array[T]): TypedCuckooTable[T] = {
    val newSize = size - size(index) + value.length
    var newData = data
    newData = newData.patch(index * bucketSize, value, math.min(bucketSize, value.length))
    if (value.length < bucketSize) {
      val zeros = ArrayOpsEx.zeros[T](bucketSize - value.length)
      newData = newData.patch(index * bucketSize + value.length, zeros, zeros.length)
    }
    val newOverflowed = overflowed.set(index, value.drop(bucketSize))
    new TreeCuckooTable[T](newData, newOverflowed, index + 1, bucketSize, newSize)
  }

  override def toString: String = {
    new TreeCuckooTable.TreeCuckooTableIterator(this)
      .zipWithIndex
      .filter(_._1.nonEmpty)
      .map(tup => tup._1.mkString(s"${tup._2}->[", ", ", "]"))
      .mkString(s"${getClass.getShortName}{", ", ", "}")
  }
}

object TreeCuckooTable {
  private final class TreeCuckooTableIterator[T](
    table: TreeCuckooTable[T],
  ) extends AbstractIterator[Array[T]] {
    private[this] var iter = 0

    override def hasNext: Boolean = iter < table.numBuckets

    override def next(): Array[T] = {
      iter += 1
      table.get(iter - 1)
    }
  }

  implicit final class ArrayMappedTrieEx(private val data: ArrayMappedTrie) extends AnyVal {
    def getUnitByClassTag[T: ClassTag](index: Int): T = getUnit(index, (implicitly[ClassTag[T]] match {
      case ClassTag.Boolean => false
      case ClassTag.Byte => 0.asInstanceOf[Byte]
      case ClassTag.Short => 0.asInstanceOf[Short]
      case ClassTag.Int => 0
      case ClassTag.Long => 0L
      case _ => throw new IllegalArgumentException()
    }).asInstanceOf[T])

    def getUnit[T](index: Int, ev: T): T = (ev match {
      case _: Boolean => ((data.get(index >>> 5) >>> (index & 31)) & 1) != 0
      case _: Byte => (data.get(index >>> 2) >>> ((index & 3) << 3)).toByte
      case _: Short => (data.get(index >>> 1) >>> ((index & 1) << 4)).toShort
      case _: Int => data.get(index)
      case _: Long => ((data.get((index << 1) + 1) & 0xFFFF_FFFFL) << 32) | (data.get(index << 1) & 0xFFFF_FFFFL)
      case _ => throw new IllegalArgumentException()
    }).asInstanceOf[T]

    def setUnit[T](index: Int, value: T): ArrayMappedTrie = value match {
      case value: Boolean => data.set(index >>> 5, data.get(index >>> 5) & ~(1 << (index & 31)) | ((if (value) 1 else 0) << (index & 31)))
      case value: Byte => data.set(index >>> 2, data.get(index >>> 2) & ~(0xFF << ((index & 3) << 3)) | ((value & 0xFF) << ((index & 3) << 3)))
      case value: Short => data.set(index >>> 1, data.get(index >>> 1) & ~(0xFFFF << ((index & 1) << 4)) | ((value & 0xFFFF) << ((index & 1) << 4)))
      case value: Int => data.set(index, value)
      case value: Long => data.set((index << 1) + 1, (value >>> 32).toInt).set(index << 1, value.toInt)
      case _ => throw new IllegalArgumentException()
    }

    def slice[T: ClassTag](from: Int, until: Int): Array[T] =
      Range.apply(from, until).map(getUnitByClassTag(_)).toArray

    def patch[T](from: Int, other: Array[T], replaced: Int): ArrayMappedTrie =
      Range.apply(0, replaced).foldLeft(data) { (thiz, i) => thiz.setUnit(from + i, other.apply(i)) }
  }
}
