package probfilter.pdsa.cuckoo

import scala.collection.immutable.TreeMap
import scala.reflect.ClassTag


/** A [[probfilter.pdsa.cuckoo.CuckooTable]] based on [[scala.collection.immutable.TreeMap]]. */
@SerialVersionUID(1L)
final class MapCuckooTable[@specialized(Specializable.Integral) T: ClassTag] private[cuckoo]
(private val data: TreeMap[Int, Array[T]], val size: Int) extends TypedCuckooTable[T] {
  def this() = this(TreeMap.empty[Int, Array[T]], 0)

  override def numBuckets: Int = if (data.isEmpty) 0 else data.lastKey + 1

  override def get(index: Int): Array[T] = data.getOrElse(index, Array.empty[T])

  override def set(index: Int, value: Array[T]): TypedCuckooTable[T] = {
    val newSize = size - size(index) + value.length
    val newData = if (value.isEmpty) data.removed(index) else data.updated(index, value)
    new MapCuckooTable[T](newData, newSize)
  }

  def toArrayCuckooTable(bucketSize: Int): ArrayCuckooTable[T] = {
    val arrayData = Array.copyOf[T](Array.empty[T], numBuckets * bucketSize)
    var arrayOverflowed: TypedCuckooTable[T] = MapCuckooTable.empty[T]
    val it = data.iterator
    while (it.hasNext) {
      val (index, bucket) = it.next()
      System.arraycopy(bucket, 0, arrayData, index * bucketSize, bucket.length)
      if (bucket.length > bucketSize)
        arrayOverflowed = arrayOverflowed.set(index, bucket.drop(bucketSize))
    }
    new ArrayCuckooTable[T](arrayData, arrayOverflowed, size, bucketSize)
  }

  override def toString: String = data.view.map(t => t._2.mkString(t._1 + "->[", ", ", "]")).mkString("MT{", ", ", "}")
}


object MapCuckooTable {
  def empty[@specialized(Specializable.Integral) T: ClassTag] = new MapCuckooTable[T]()
}
