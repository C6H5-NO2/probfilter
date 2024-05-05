package probfilter.pdsa.cuckoo.mutable

import probfilter.pdsa.cuckoo.ArrayCuckooTableOps
import probfilter.util.ArrayOpsEx

import scala.reflect.ClassTag


/** A [[probfilter.pdsa.cuckoo.mutable.CuckooTable]] based on [[scala.Array]]. */
@SerialVersionUID(1L)
final class ArrayCuckooTable[T: ClassTag] private[mutable]
(private var data: Array[T], private val overflowed: TypedCuckooTable[T], private var currSize: Int, private val bucketSize: Int)
  extends TypedCuckooTable[T] {
  def this(numBuckets: Int, bucketSize: Int) =
    this(ArrayOpsEx.zeros[T](numBuckets * bucketSize), new MapCuckooTable[T](), 0, bucketSize)

  override def numBuckets: Int = ArrayCuckooTableOps.numBuckets(data, bucketSize)

  override def size: Int = currSize

  override def get(index: Int): Array[T] = ArrayCuckooTableOps.get(data, overflowed, bucketSize, index)

  override def set(index: Int, value: Array[T]): TypedCuckooTable[T] = {
    currSize = currSize - size(index) + value.length
    data = if (index < numBuckets) data else Array.copyOf(data, (index + 1) * bucketSize)
    ArrayCuckooTableOps.setData(data, bucketSize, index, value)
    overflowed.set(index, value.drop(bucketSize))
    this
  }

  def toMapCuckooTable: MapCuckooTable[T] =
    ArrayCuckooTableOps.toMapCuckooTable(this, MapCuckooTable.empty[T]).asInstanceOf[MapCuckooTable[T]]

  override def toString: String = ArrayCuckooTableOps.toString(this)
}


object ArrayCuckooTable {
  def empty[T: ClassTag]: ArrayCuckooTable[T] = new ArrayCuckooTable[T](16, 4)
}
