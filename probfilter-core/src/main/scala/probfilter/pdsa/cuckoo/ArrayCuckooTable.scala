package probfilter.pdsa.cuckoo

import scala.collection.AbstractIterator
import scala.reflect.ClassTag


/**
 * A [[probfilter.pdsa.cuckoo.CuckooTable]] based on [[scala.Array]].
 *
 * @note Mutation always copies the whole array.
 */
@SerialVersionUID(1L)
final class ArrayCuckooTable[@specialized(Specializable.Integral) T: ClassTag] private[cuckoo]
(private val data: Array[T], private val overflowed: TypedCuckooTable[T], val size: Int, private val bucketSize: Int)
  extends TypedCuckooTable[T] {
  def this(numBuckets: Int, bucketSize: Int) =
    this(Array.copyOf[T](Array.empty[T], numBuckets * bucketSize), new MapCuckooTable[T](), 0, bucketSize)

  override def numBuckets: Int = data.length / bucketSize

  override def get(index: Int): Array[T] = {
    val from = index * bucketSize
    val d = data.slice(from, from + bucketSize).filter(_.asInstanceOf[Long] != 0L)
    val o = overflowed.get(index)
    d.concat(o)
  }

  override def set(index: Int, value: Array[T]): TypedCuckooTable[T] = {
    setImpl(index, value.filter(_.asInstanceOf[Long] != 0L))
  }

  private def setImpl(index: Int, value: Array[T]): TypedCuckooTable[T] = {
    val newSize = size - size(index) + value.length
    val newData = if (index < numBuckets) data.clone() else Array.copyOf[T](data, (index + 1) * bucketSize)
    var i = 0
    val from = index * bucketSize
    val hi = math.min(bucketSize, value.length)
    while (i < hi) {
      newData.update(from + i, value.apply(i))
      i += 1
    }
    while (i < bucketSize) {
      newData.update(from + i, 0L.asInstanceOf[T])
      i += 1
    }
    val newOverflowed = overflowed.set(index, value.drop(bucketSize))
    new ArrayCuckooTable[T](newData, newOverflowed, newSize, bucketSize)
  }

  def toMapCuckooTable: MapCuckooTable[T] = {
    var mapTable: TypedCuckooTable[T] = MapCuckooTable.empty[T]
    var i = 0
    while (i < numBuckets) {
      mapTable = mapTable.set(i, get(i))
      i += 1
    }
    mapTable.asInstanceOf[MapCuckooTable[T]]
  }

  override def toString: String = {
    new AbstractIterator[Array[T]] {
      var i = 0

      override def hasNext: Boolean = i < numBuckets

      override def next(): Array[T] = {
        i += 1
        get(i - 1)
      }
    }.zipWithIndex.filter(_._1.nonEmpty).map(t => t._1.mkString(t._2 + "->[", ", ", "]")).mkString("AT{", ", ", "}")
  }
}


object ArrayCuckooTable {
  def empty[@specialized(Specializable.Integral) T: ClassTag] = new ArrayCuckooTable[T](16, 4)
}
