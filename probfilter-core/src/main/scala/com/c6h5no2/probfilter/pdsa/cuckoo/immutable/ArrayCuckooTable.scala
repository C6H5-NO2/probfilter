package probfilter.pdsa.cuckoo.immutable

import probfilter.pdsa.cuckoo.ArrayCuckooTableOps
import probfilter.util.ArrayOpsEx

import scala.reflect.ClassTag


/**
 * A [[probfilter.pdsa.cuckoo.immutable.CuckooTable]] based on [[scala.Array]].
 *
 * @note Mutations always copy the whole array.
 */
@SerialVersionUID(1L)
final class ArrayCuckooTable[T: ClassTag] private[immutable]
(private val data: Array[T], private val overflowed: TypedCuckooTable[T], val size: Int, private val bucketSize: Int)
  extends TypedCuckooTable[T] {
  def this(numBuckets: Int, bucketSize: Int) =
    this(ArrayOpsEx.zeros[T](numBuckets * bucketSize), new MapCuckooTable[T](), 0, bucketSize)

  override def numBuckets: Int = ArrayCuckooTableOps.numBuckets(data, bucketSize)

  override def get(index: Int): Array[T] = ArrayCuckooTableOps.get(data, overflowed, bucketSize, index)

  override def set(index: Int, value: Array[T]): TypedCuckooTable[T] = {
    val newSize = size - size(index) + value.length
    val newData = if (index < numBuckets) data.clone() else Array.copyOf(data, (index + 1) * bucketSize)
    ArrayCuckooTableOps.setData(newData, bucketSize, index, value)
    val newOverflowed = overflowed.set(index, value.drop(bucketSize))
    new ArrayCuckooTable[T](newData, newOverflowed, newSize, bucketSize)
  }

  def toMapCuckooTable: MapCuckooTable[T] =
    ArrayCuckooTableOps.toMapCuckooTable(this, MapCuckooTable.empty[T]).asInstanceOf[MapCuckooTable[T]]

  override def toString: String = ArrayCuckooTableOps.toString(this)
}


object ArrayCuckooTable {
  /** @return an array cuckoo table with 16 four-slot-buckets */
  def empty[T: ClassTag]: ArrayCuckooTable[T] = new ArrayCuckooTable[T](16, 4)
}
