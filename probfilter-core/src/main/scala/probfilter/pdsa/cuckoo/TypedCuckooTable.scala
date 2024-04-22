package probfilter.pdsa.cuckoo

import probfilter.util.FalseRandom

import scala.reflect.ClassTag


/** A [[probfilter.pdsa.cuckoo.CuckooTable]] specialized on [[scala.Byte]], [[scala.Short]], [[scala.Int]], and [[scala.Long]]. */
trait TypedCuckooTable[@specialized(Specializable.Integral) T] extends CuckooTable {
  /** @return number of buckets in the whole table */
  def numBuckets: Int

  /** @return number of entries in the whole table */
  def size: Int

  /** @return a copy of entries in the bucket at `index` */
  def get(index: Int): Array[T]

  def set(index: Int, value: Array[T]): TypedCuckooTable[T]

  /** @return number of entries in the bucket at `index` */
  def size(index: Int): Int = get(index).length

  def contains(index: Int, entry: T): Boolean = get(index).contains(entry)

  def exists(index: Int, p: T => Boolean): Boolean = get(index).exists(p)

  /**
   * @return a table with entry `entry` added to the bucket at `index`
   * @throws java.lang.IllegalArgumentException if `entry` is zero
   */
  def add(index: Int, entry: T): TypedCuckooTable[T] = {
    require(entry != 0L.asInstanceOf[T])
    set(index, TypedCuckooTable.arrayAppended[T](get(index), entry))
  }

  /**
   * @return a table with entry `entry` removed from the bucket at `index`
   * @throws java.lang.IllegalArgumentException if `entry` is zero
   */
  def remove(index: Int, entry: T): TypedCuckooTable[T] = {
    require(entry != 0L.asInstanceOf[T])
    set(index, get(index).filter(_ != entry))
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
    val newTable = set(index, TypedCuckooTable.arrayUpdated[T](arr, victimIndex, entry))
    (replaced, newTable)
  }

  def replace(index: Int, entry: T): (T, TypedCuckooTable[T]) = replace(index, entry, FalseRandom.next(size(index)))

  /**
   * Similar to [[scala.collection.Iterable.zip]] + [[scala.collection.Iterable.zipWithIndex]] + [[scala.collection.Iterable.foldLeft]], yet combining all buckets.
   *
   * @param that the other cuckoo table to be zipped with
   * @param z the start value
   * @param op a [[scala.Function4]] whose params are: the current value, entries from `this`, entries from `that`, and bucket index
   */
  def zipFold[B](that: TypedCuckooTable[T])(z: B)(op: (B, Array[T], Array[T], Int) => B): B = {
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


object TypedCuckooTable {
  def empty[@specialized(Specializable.Integral) T: ClassTag]: TypedCuckooTable[T] = MapCuckooTable.empty[T]

  /** Equivalent to [[scala.collection.ArrayOps.appended]] without requiring [[scala.reflect.ClassTag]]. */
  private def arrayAppended[@specialized(Specializable.Integral) T](array: Array[T], elem: T): Array[T] = {
    val dest = Array.copyOf[T](array, array.length + 1)
    dest.update(array.length, elem)
    dest
  }

  /** Equivalent to [[scala.collection.ArrayOps.updated]] without requiring [[scala.reflect.ClassTag]]. */
  private def arrayUpdated[@specialized(Specializable.Integral) T](array: Array[T], index: Int, elem: T): Array[T] = {
    val dest = array.clone()
    dest.update(index, elem)
    dest
  }
}
