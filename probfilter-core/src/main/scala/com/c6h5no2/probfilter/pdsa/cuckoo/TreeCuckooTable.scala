package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.util.ArrayOpsEx
import com.c6h5no2.probfilter.util.ClassEx.Clazz

import scala.collection.AbstractIterator
import scala.reflect.{ClassTag, classTag}


/**
 * A tree-based solution for immutable cuckoo table in upcoming versions.
 * Currently use Vector as a place holder.
 */
@SerialVersionUID(1L)
final class TreeCuckooTable[T: ClassTag] private(
  data: Vector[T],
  overflowed: MapCuckooTable[T],
  val numBuckets: Int,
  val bucketSize: Int,
  val size: Int,
) extends TypedCuckooTable[T] {
  def this(numBuckets: Int, bucketSize: Int) = this(
    Vector.empty[T],
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
    var newData = if (index < data.length / bucketSize) data else data.padTo((index + 1) * bucketSize, 0L.asInstanceOf[T])
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
}
