package probfilter.pdsa.cuckoo.immutable

import probfilter.pdsa.cuckoo.MapCuckooTableOps

import scala.collection.immutable.TreeMap
import scala.reflect.ClassTag


/** A [[probfilter.pdsa.cuckoo.immutable.CuckooTable]] based on [[scala.collection.immutable.TreeMap]]. */
@SerialVersionUID(1L)
final class MapCuckooTable[T: ClassTag] private
(private val data: TreeMap[Int, Array[T]], val size: Int)
  extends TypedCuckooTable[T] {
  def this() = this(TreeMap.empty[Int, Array[T]], 0)

  override def numBuckets: Int = MapCuckooTableOps.numBuckets(data)

  override def get(index: Int): Array[T] = MapCuckooTableOps.get(data, index)

  override def set(index: Int, value: Array[T]): TypedCuckooTable[T] = {
    val newSize = size - size(index) + value.length
    val newData = if (value.isEmpty) data.removed(index) else data.updated(index, value)
    new MapCuckooTable[T](newData, newSize)
  }

  def toArrayCuckooTable(bucketSize: Int): ArrayCuckooTable[T] = {
    val tup = MapCuckooTableOps.toArrayCuckooTableData(data, bucketSize, MapCuckooTable.empty[T])
    new ArrayCuckooTable[T](tup._1, tup._2.asInstanceOf[TypedCuckooTable[T]], size, bucketSize)
  }

  override def toString: String = MapCuckooTableOps.toString(data)
}


object MapCuckooTable {
  def empty[T: ClassTag]: MapCuckooTable[T] = new MapCuckooTable[T]()
}
