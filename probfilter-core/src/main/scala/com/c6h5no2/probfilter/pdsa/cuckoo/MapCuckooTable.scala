package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.util.ClassEx.Clazz
import com.c6h5no2.probfilter.util.{ArrayOpsEx, Immutable => ImmCol, Mutable => MutCol}

import scala.collection.{SortedMap, immutable, mutable}
import scala.reflect.{ClassTag, classTag}


/**
 * A [[CuckooTable]] based on [[scala.collection.immutable.TreeMap immutable]] or
 * [[scala.collection.mutable.TreeMap mutable]] [[scala.collection.SortedMap]].
 */
sealed trait MapCuckooTable[T] extends TypedCuckooTable[T] {
  override final def numBuckets: Int = if (data.isEmpty) 0 else data.lastKey + 1

  override final def get(index: Int): Array[T] = data.get(index).fold(Array.empty[T](storageType))(_.clone())

  override def set(index: Int, value: Array[T]): MapCuckooTable[T]

  def toArrayCuckooTable: ArrayCuckooTable[T]

  protected def data: SortedMap[Int, Array[T]]

  override def toString: String = {
    data
      .view
      .map(tup => tup._2.mkString(s"${tup._1}->[", ", ", "]"))
      .mkString(s"${getClass.getShortName}{", ", ", "}")
  }
}

object MapCuckooTable {
  def apply[T](
    mutable: Boolean,
    numBuckets: Int,
    bucketSize: Int,
    storageType: ClassTag[T],
  ): MapCuckooTable[T] = {
    if (mutable)
      new MapCuckooTable.Mutable[T](numBuckets, bucketSize)(storageType)
    else
      new MapCuckooTable.Immutable[T](numBuckets, bucketSize)(storageType)
  }

  @SerialVersionUID(1L)
  final class Immutable[T: ClassTag] private(
    protected val data: immutable.TreeMap[Int, Array[T]],
    val bucketSize: Int,
    val size: Int,
  ) extends MapCuckooTable[T]
    with ImmCol {
    def this(bucketSize: Int) = this(immutable.TreeMap.empty[Int, Array[T]], bucketSize, 0)

    def this(numBuckets: Int, bucketSize: Int) = this(bucketSize)

    override def storageType: ClassTag[T] = classTag[T]

    override def set(index: Int, value: Array[T]): MapCuckooTable[T] = {
      val newSize = size - size(index) + value.length
      val newData = if (value.isEmpty) data.removed(index) else data.updated(index, value.clone())
      new MapCuckooTable.Immutable[T](newData, bucketSize, newSize)
    }

    override def toArrayCuckooTable: ArrayCuckooTable[T] = {
      val tup = toArrayCuckooTableData(this, new MapCuckooTable.Immutable[T](bucketSize))
      new ArrayCuckooTable.Immutable[T](tup._1, tup._2, bucketSize, size)
    }
  }

  @SerialVersionUID(1L)
  final class Mutable[T: ClassTag] private(
    protected val data: mutable.TreeMap[Int, Array[T]],
    val bucketSize: Int,
    private[this] var _size: Int,
  ) extends MapCuckooTable[T]
    with MutCol {
    def this(bucketSize: Int) = this(mutable.TreeMap.empty[Int, Array[T]], bucketSize, 0)

    def this(numBuckets: Int, bucketSize: Int) = this(bucketSize)

    override def storageType: ClassTag[T] = classTag[T]

    override def size: Int = _size

    override def set(index: Int, value: Array[T]): MapCuckooTable[T] = {
      this._size = size - size(index) + value.length
      if (value.isEmpty) data.subtractOne(index) else data.addOne((index, value.clone()))
      this
    }

    override def toArrayCuckooTable: ArrayCuckooTable[T] = {
      val tup = toArrayCuckooTableData(this, new MapCuckooTable.Mutable[T](bucketSize))
      new ArrayCuckooTable.Mutable[T](tup._1, tup._2, bucketSize, size)
    }
  }

  private def toArrayCuckooTableData[T](
    srcTable: MapCuckooTable[T],
    emptyTable: TypedCuckooTable[T],
  ): (Array[T], TypedCuckooTable[T]) = {
    val arrayData = ArrayOpsEx.zeros(srcTable.numBuckets * srcTable.bucketSize)(srcTable.storageType)
    val arrayOverflowed = emptyTable
    val iter = srcTable.data.iterator
    iter.foldLeft((arrayData, arrayOverflowed)) { (arrayTable, tup) =>
      val arrayData = arrayTable._1
      var arrayOverflowed = arrayTable._2
      val index = tup._1
      val bucket = tup._2
      ArrayOpsEx.copyTo(bucket, 0, arrayData, index * srcTable.bucketSize, bucket.length)
      if (bucket.length > srcTable.bucketSize) {
        arrayOverflowed = arrayOverflowed.set(index, bucket.drop(srcTable.bucketSize))
      }
      (arrayData, arrayOverflowed)
    }
  }
}
