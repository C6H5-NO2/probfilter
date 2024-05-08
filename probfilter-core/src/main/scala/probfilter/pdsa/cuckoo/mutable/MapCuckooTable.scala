package probfilter.pdsa.cuckoo.mutable

import probfilter.pdsa.cuckoo.MapCuckooTableOps

import scala.collection.mutable
import scala.reflect.ClassTag


/** A [[probfilter.pdsa.cuckoo.mutable.CuckooTable]] based on [[scala.collection.mutable.TreeMap]]. */
@SerialVersionUID(1L)
final class MapCuckooTable[T: ClassTag] private
(private val data: mutable.TreeMap[Int, Array[T]], private var currSize: Int)
  extends TypedCuckooTable[T] {
  def this() = this(mutable.TreeMap.empty[Int, Array[T]], 0)

  override def numBuckets: Int = MapCuckooTableOps.numBuckets(data)

  override def size: Int = currSize

  override def get(index: Int): Array[T] = MapCuckooTableOps.get(data, index)

  override def set(index: Int, value: Array[T]): TypedCuckooTable[T] = {
    currSize = currSize - size(index) + value.length
    if (value.isEmpty) data.subtractOne(index) else data.addOne((index, value))
    this
  }

  override def reserve(buckets: Int): TypedCuckooTable[T] = this

  def toArrayCuckooTable(bucketSize: Int): ArrayCuckooTable[T] = {
    val tup = MapCuckooTableOps.toArrayCuckooTableData(data, bucketSize, MapCuckooTable.empty[T])
    new ArrayCuckooTable[T](tup._1, tup._2.asInstanceOf[TypedCuckooTable[T]], currSize, bucketSize)
  }

  override def toString: String = MapCuckooTableOps.toString(data)
}


object MapCuckooTable {
  def empty[T: ClassTag]: MapCuckooTable[T] = new MapCuckooTable[T]()
}
