package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.util.ArrayOpsEx

import scala.reflect.ClassTag


/** @note DO NOT extend this trait; extend [[TypedCuckooTable]]. */
private[cuckoo] trait TypedCuckooTableOps[T] {
  // assigning self-type will trigger cyclic reference

  /** @return primitive storage type of entries (e.g. [[scala.reflect.ClassTag$.Long]]) */
  def storageType: ClassTag[T]

  /** @return number of buckets in the whole table */
  def numBuckets: Int

  /** @return expected number of slots per bucket */
  def bucketSize: Int

  /** @return number of entries in the whole table */
  def size: Int

  /** Optionally reserves the number of buckets to `buckets`. */
  def reserve(buckets: Int): TypedCuckooTable[T] = this.asInstanceOf[TypedCuckooTable[T]]

  /** @return a <i>copy</i> of entries in the bucket at `index` */
  def get(index: Int): Array[T]

  /** Sets the contents of bucket at `index` to entries in `value`. */
  def set(index: Int, value: Array[T]): TypedCuckooTable[T]

  /** @return number of entries in the bucket at `index` */
  def size(index: Int): Int = get(index).length

  /** @return `true` if `entry` is contained in the bucket at `index` */
  def contains(index: Int, entry: T): Boolean = get(index).contains(entry) // boxing here

  /** @return `true` if `predicate` is satisfied by any entry in the bucket at `index` */
  def exists(index: Int, predicate: T => Boolean): Boolean = get(index).exists(predicate) // boxing here

  /**
   * @param entry a non-zero entry
   * @return a table with entry `entry` added to the bucket at `index`
   */
  def add(index: Int, entry: T): TypedCuckooTable[T] = {
    val arr = get(index)
    val dest = ArrayOpsEx.appended(arr, entry)
    set(index, dest)
  }

  /**
   * @param entry a non-zero entry
   * @return a table with (at most) 1 instance of entry `entry` removed from the bucket at `index`
   */
  def remove(index: Int, entry: T): TypedCuckooTable[T] = {
    val arr = get(index)
    // boxing here; see `scala.runtime.BoxesRunTime.equals`
    val idx = arr.indexOf(entry)
    if (idx == -1) {
      this.asInstanceOf[TypedCuckooTable[T]]
    } else {
      val dest = ArrayOpsEx.removedAt(arr, idx)
      set(index, dest)
    }
  }

  /**
   * Replaces the entry at index `victimIndex` within the bucket at `index` by `entry`.
   *
   * @return a [[scala.Tuple2]] of the replaced entry and the updated table
   * @throws java.lang.IndexOutOfBoundsException if `victimIndex` is out of range
   */
  def replace(index: Int, entry: T, victimIndex: Int): (T, TypedCuckooTable[T]) = {
    val arr = get(index)
    val replaced = arr.apply(victimIndex)
    val dest = ArrayOpsEx.updated(arr, victimIndex, entry)
    val newTable = set(index, dest)
    (replaced, newTable)
  }

  /**
   * Analogous to [[scala.collection.Iterable.zip zip]] + [[scala.collection.Iterable.zipWithIndex zipWithIndex]] +
   * [[scala.collection.Iterable.foldLeft foldLeft]], except that all buckets are combined.
   *
   * @param that the other cuckoo table to be zipped with
   * @param z the start value
   * @param op a [[scala.Function4]] whose parameters are: the current value, entries from `this`,
   * entries from `that`, and bucket index, and the return value is the next value
   */
  def zipFold[B](that: TypedCuckooTable[T])(z: B)(op: (B, Array[T], Array[T], Int) => B): B = {
    var result = z
    var index = 0
    val max = math.max(this.numBuckets, that.numBuckets)
    while (index < max) {
      result = op.apply(result, this.get(index), that.get(index), index)
      index += 1
    }
    result
  }
}
