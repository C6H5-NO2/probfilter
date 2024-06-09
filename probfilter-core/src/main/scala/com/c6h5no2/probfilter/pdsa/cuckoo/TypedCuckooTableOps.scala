package probfilter.pdsa.cuckoo

import probfilter.util.ArrayOpsEx


trait TypedCuckooTableOps[T] extends CuckooTableOps {
  /** @return number of buckets in the whole table */
  def numBuckets: Int

  /** @return number of entries in the whole table */
  def size: Int

  /** @return a <i>copy</i> of entries in the bucket at `index` */
  def get(index: Int): Array[T]

  def set(index: Int, value: Array[T]): TypedCuckooTableOps[T]

  /** Optionally reserves the number of buckets to `buckets`. */
  def reserve(buckets: Int): TypedCuckooTableOps[T]

  /** @return number of entries in the bucket at `index` */
  def size(index: Int): Int = get(index).length

  def contains(index: Int, entry: T): Boolean = get(index).contains(entry) // boxing

  def exists(index: Int, p: T => Boolean): Boolean = get(index).exists(p) // boxing

  /**
   * @param entry a non-zero entry
   * @return a table with entry `entry` added to the bucket at `index`
   */
  def add(index: Int, entry: T): TypedCuckooTableOps[T] = {
    val arr = get(index)
    val dest = ArrayOpsEx.appended(arr, entry)
    set(index, dest)
  }

  /**
   * @param entry a non-zero entry
   * @return a table with (at most) 1 instance of entry `entry` removed from the bucket at `index`
   */
  def remove(index: Int, entry: T): TypedCuckooTableOps[T] = {
    val arr = get(index)
    /** boxing here; see [[scala.runtime.BoxesRunTime.equals]] */
    val idx = arr.indexOf(entry)
    if (idx == -1) {
      this
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
  def replace(index: Int, entry: T, victimIndex: Int): (T, TypedCuckooTableOps[T]) = {
    val arr = get(index)
    val replaced = arr.apply(victimIndex)
    val dest = ArrayOpsEx.updated(arr, victimIndex, entry)
    val newTable = set(index, dest)
    (replaced, newTable)
  }

  /**
   * Similar to [[scala.collection.Iterable.zip]] + [[scala.collection.Iterable.zipWithIndex]] + [[scala.collection.Iterable.foldLeft]], yet combining all buckets.
   *
   * @param that the other cuckoo table to be zipped with
   * @param z the start value
   * @param op a [[scala.Function4]] whose params are: the current value, entries from `this`, entries from `that`, and bucket index
   */
  def zipFold[B](that: TypedCuckooTableOps[T])(z: B)(op: (B, Array[T], Array[T], Int) => B): B = {
    var result = z
    var index = 0
    val max = math.max(this.numBuckets, that.numBuckets)
    while (index < max) {
      result = op(result, this.get(index), that.get(index), index)
      index += 1
    }
    result
  }
}
