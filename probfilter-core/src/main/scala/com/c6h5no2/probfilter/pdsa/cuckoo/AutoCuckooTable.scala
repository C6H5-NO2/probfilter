package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.util.{Immutable => ImmCol, Mutable => MutCol}

import scala.reflect.ClassTag


/** A [[CuckooTable]] that automatically switches between [[MapCuckooTable]] and [[ArrayCuckooTable]]. */
sealed trait AutoCuckooTable[T] extends TypedCuckooTable[T] {
  override final def storageType: ClassTag[T] = data.storageType

  override final def numBuckets: Int = data.numBuckets

  override final def bucketSize: Int = data.bucketSize

  override final def size: Int = data.size

  override final def reserve(buckets: Int): AutoCuckooTable[T] = copy(data.reserve(buckets))

  override final def get(index: Int): Array[T] = data.get(index)

  override final def set(index: Int, value: Array[T]): AutoCuckooTable[T] = {
    val newSize = size - size(index) + value.length
    val newData = data match {
      case data: ArrayCuckooTable[T] if (newSize < AutoCuckooTable.toMapAt * capacity) =>
        data.toMapCuckooTable.set(index, value)
      case data: MapCuckooTable[T] if (newSize > AutoCuckooTable.toArrayAt * capacity) =>
        data.set(index, value).toArrayCuckooTable
      case _ =>
        data.set(index, value)
    }
    copy(newData)
  }

  protected def data: TypedCuckooTable[T]

  /** @return expected number of entries to hold */
  protected def capacity: Int

  protected def copy(data: TypedCuckooTable[T]): AutoCuckooTable[T]
}

object AutoCuckooTable {
  private val toMapAt: Double = 0.4
  private val toArrayAt: Double = 0.6

  def apply[T](
    mutable: Boolean,
    numBuckets: Int,
    bucketSize: Int,
    storageType: ClassTag[T],
  ): AutoCuckooTable[T] = {
    if (mutable)
      new AutoCuckooTable.Mutable[T](numBuckets, bucketSize)(storageType)
    else
      new AutoCuckooTable.Immutable[T](numBuckets, bucketSize)(storageType)
  }

  @SerialVersionUID(1L)
  final class Immutable[T: ClassTag] private(
    protected val data: TypedCuckooTable[T],
    protected val capacity: Int,
  ) extends AutoCuckooTable[T]
    with ImmCol {
    def this(numBuckets: Int, bucketSize: Int) =
      this(new MapCuckooTable.Immutable[T](bucketSize), numBuckets * bucketSize)

    override protected def copy(data: TypedCuckooTable[T]): AutoCuckooTable[T] = {
      new AutoCuckooTable.Immutable[T](data, this.capacity)
    }
  }

  @SerialVersionUID(1L)
  final class Mutable[T: ClassTag] private(
    protected var data: TypedCuckooTable[T],
    protected val capacity: Int,
  ) extends AutoCuckooTable[T]
    with MutCol {
    def this(numBuckets: Int, bucketSize: Int) =
      this(new MapCuckooTable.Mutable[T](bucketSize), numBuckets * bucketSize)

    override protected def copy(data: TypedCuckooTable[T]): AutoCuckooTable[T] = {
      this.data = data
      this
    }
  }
}
