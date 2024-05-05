package probfilter.pdsa.cuckoo

import probfilter.util.ArrayOpsEx

import scala.collection.AbstractIterator


object ArrayCuckooTableOps {
  def numBuckets(data: Array[_], bucketSize: Int): Int = data.length / bucketSize

  def get[T](data: Array[T], overflowed: TypedCuckooTableOps[T], bucketSize: Int, index: Int): Array[T] = {
    val from = index * bucketSize
    // compiled as `!scala.runtime.BoxesRunTime.equals((Object) x, scala.runtime.BoxesRunTime.boxToLong(0L))`
    val d = data.slice(from, from + bucketSize).filter(_ != 0L.asInstanceOf[T])
    val o = overflowed.get(index)
    ArrayOpsEx.concated(d, o)
  }

  def setData[T](data: Array[T], bucketSize: Int, index: Int, value: Array[T]): Unit = {
    var i = 0
    val from = index * bucketSize
    val hi = math.min(bucketSize, value.length)
    while (i < hi) {
      data.update(from + i, value.apply(i))
      i += 1
    }
    val zero = ArrayOpsEx.boxedZero(data).asInstanceOf[T]
    while (i < bucketSize) {
      data.update(from + i, zero)
      i += 1
    }
  }

  def toMapCuckooTable[T](arrayTable: TypedCuckooTableOps[T], emptyMapTable: TypedCuckooTableOps[T]): TypedCuckooTableOps[T] = {
    var i = 0
    var mapTable = emptyMapTable
    while (i < arrayTable.numBuckets) {
      mapTable = mapTable.set(i, arrayTable.get(i))
      i += 1
    }
    mapTable
  }

  def toString[T](table: TypedCuckooTableOps[T]): String = {
    new AbstractIterator[Array[T]] {
      private var i = 0

      override def hasNext: Boolean = i < table.numBuckets

      override def next(): Array[T] = {
        i += 1
        table.get(i - 1)
      }
    }.zipWithIndex.filter(_._1.nonEmpty).map(t => t._1.mkString(t._2 + "->[", ", ", "]")).mkString("AT{", ", ", "}")
  }
}
