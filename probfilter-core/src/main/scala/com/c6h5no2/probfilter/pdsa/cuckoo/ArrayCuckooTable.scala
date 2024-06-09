package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.util.ClassEx.Clazz
import com.c6h5no2.probfilter.util.{ArrayOpsEx, Immutable => ImmCol, Mutable => MutCol}

import scala.collection.AbstractIterator
import scala.reflect.{ClassTag, classTag}


/** A [[CuckooTable]] based on [[scala.Array]]. */
sealed trait ArrayCuckooTable[T] extends TypedCuckooTable[T] {
  override final def numBuckets: Int = data.length / bucketSize

  override final def get(index: Int): Array[T] = {
    val from = index * bucketSize
    // The lambda function is compiled to
    // `!scala.runtime.BoxesRunTime.equals((Object) x, scala.runtime.BoxesRunTime.boxToLong(0L))`
    // which in turn calls
    // `scala.runtime.BoxesRunTime.equalsNumNum(java.lang.Number, java.lang.Number)`
    // where their `java.lang.Number.longValue()`s are compared.
    val d = data.slice(from, from + bucketSize).filter(_ != 0L.asInstanceOf[T])
    val o = overflowed.get(index)
    ArrayOpsEx.concated(d, o)
  }

  override def set(index: Int, value: Array[T]): ArrayCuckooTable[T]

  def toMapCuckooTable: MapCuckooTable[T]

  protected def data: Array[T]

  protected def overflowed: TypedCuckooTable[T]

  override def toString: String = {
    val iter = new ArrayCuckooTable.ArrayCuckooTableIterator(this)
    iter
      .zipWithIndex
      .filter(_._1.nonEmpty)
      .map(tup => tup._1.mkString(s"${tup._2}->[", ", ", "]"))
      .mkString(s"${getClass.getShortName}{", ", ", "}")
  }
}

object ArrayCuckooTable {
  def apply[T](
    mutable: Boolean,
    numBuckets: Int,
    bucketSize: Int,
    storageType: ClassTag[T],
  ): ArrayCuckooTable[T] = {
    if (mutable)
      new ArrayCuckooTable.Mutable[T](numBuckets, bucketSize)(storageType)
    else
      new ArrayCuckooTable.Immutable[T](numBuckets, bucketSize)(storageType)
  }

  /**
   * @note "Mutation" always copies the whole array.
   *       [[ArrayCuckooTable.Mutable]] is preferable in terms of performance.
   */
  @SerialVersionUID(1L)
  final class Immutable[T: ClassTag] private[cuckoo](
    protected val data: Array[T],
    protected val overflowed: TypedCuckooTable[T],
    val bucketSize: Int,
    val size: Int,
  ) extends ArrayCuckooTable[T]
    with ImmCol {
    def this(numBuckets: Int, bucketSize: Int) = this(
      ArrayOpsEx.zeros[T](numBuckets * bucketSize),
      new MapCuckooTable.Immutable[T](bucketSize),
      bucketSize,
      0
    )

    override def storageType: ClassTag[T] = classTag[T]

    override def set(index: Int, value: Array[T]): ArrayCuckooTable[T] = {
      val newSize = size - size(index) + value.length
      val newData = if (index < numBuckets) data.clone() else Array.copyOf(data, (index + 1) * bucketSize)
      ArrayCuckooTable.setData(newData, bucketSize, index, value)
      val newOverflowed = overflowed.set(index, value.drop(bucketSize))
      new ArrayCuckooTable.Immutable[T](newData, newOverflowed, bucketSize, newSize)
    }

    override def toMapCuckooTable: MapCuckooTable[T] =
      ArrayCuckooTable.toMapCuckooTable[T](this, new MapCuckooTable.Immutable[T](bucketSize))
  }

  @SerialVersionUID(1L)
  final class Mutable[T: ClassTag] private[cuckoo](
    protected var data: Array[T],
    protected val overflowed: TypedCuckooTable[T],
    val bucketSize: Int,
    private[this] var _size: Int,
  ) extends ArrayCuckooTable[T]
    with MutCol {
    def this(numBuckets: Int, bucketSize: Int) = this(
      ArrayOpsEx.zeros[T](numBuckets * bucketSize),
      new MapCuckooTable.Mutable[T](bucketSize),
      bucketSize,
      0
    )

    override def storageType: ClassTag[T] = classTag[T]

    override def size: Int = _size

    override def reserve(buckets: Int): ArrayCuckooTable[T] = {
      data = if (buckets <= numBuckets) data else Array.copyOf(data, buckets * bucketSize)
      overflowed.reserve(buckets)
      this
    }

    override def set(index: Int, value: Array[T]): ArrayCuckooTable[T] = {
      this._size = size - size(index) + value.length
      data = if (index < numBuckets) data else Array.copyOf(data, (index + 1) * bucketSize)
      ArrayCuckooTable.setData(data, bucketSize, index, value)
      overflowed.set(index, value.drop(bucketSize))
      this
    }

    override def toMapCuckooTable: MapCuckooTable[T] =
      ArrayCuckooTable.toMapCuckooTable[T](this, new MapCuckooTable.Mutable[T](bucketSize))
  }

  /** @param data data to be set in-place */
  private def setData[T](data: Array[T], bucketSize: Int, index: Int, value: Array[T]): Unit = {
    var i = 0
    val from = index * bucketSize
    val hi = math.min(bucketSize, value.length)
    while (i < hi) {
      data.update(from + i, value.apply(i))
      i += 1
    }
    val zero = ArrayOpsEx.boxedZero(data).asInstanceOf[T]
    while (i < bucketSize) {
      data.update(from + i, zero)
      i += 1
    }
  }

  private def toMapCuckooTable[T](
    srcTable: ArrayCuckooTable[T],
    emptyTable: MapCuckooTable[T],
  ): MapCuckooTable[T] = {
    Range.apply(0, srcTable.numBuckets).foldLeft(emptyTable) { (mapTable, index) =>
      mapTable.set(index, srcTable.get(index))
    }
  }

  private final class ArrayCuckooTableIterator[T](
    table: ArrayCuckooTable[T],
  ) extends AbstractIterator[Array[T]] {
    private[this] var iter = 0

    override def hasNext: Boolean = iter < table.numBuckets

    override def next(): Array[T] = {
      iter += 1
      table.get(iter - 1)
    }
  }
}
