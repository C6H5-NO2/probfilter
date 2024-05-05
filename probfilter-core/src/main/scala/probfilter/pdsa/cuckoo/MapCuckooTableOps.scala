package probfilter.pdsa.cuckoo

import probfilter.util.ArrayOpsEx

import scala.collection.SortedMap
import scala.reflect.ClassTag


private[cuckoo] object MapCuckooTableOps {
  def numBuckets(data: SortedMap[Int, _]): Int = if (data.isEmpty) 0 else data.lastKey + 1

  def get[T: ClassTag](data: SortedMap[Int, Array[T]], index: Int): Array[T] = data.getOrElse(index, Array.empty[T])

  def toArrayCuckooTableData[T: ClassTag](data: SortedMap[Int, Array[T]], bucketSize: Int,
                                          emptyTable: TypedCuckooTableOps[T]): (Array[T], TypedCuckooTableOps[T]) = {
    val arrayData = ArrayOpsEx.zeros[T](numBuckets(data) * bucketSize)
    var arrayOverflowed = emptyTable
    val it = data.iterator
    while (it.hasNext) {
      val tup = it.next()
      val index = tup._1
      val bucket = tup._2
      ArrayOpsEx.copy(bucket, 0, arrayData, index * bucketSize, bucket.length)
      if (bucket.length > bucketSize)
        arrayOverflowed = arrayOverflowed.set(index, bucket.drop(bucketSize))
    }
    (arrayData, arrayOverflowed)
  }

  def toString[T](data: SortedMap[Int, Array[T]]): String =
    data.view.map(t => t._2.mkString(t._1 + "->[", ", ", "]")).mkString("MT{", ", ", "}")
}
