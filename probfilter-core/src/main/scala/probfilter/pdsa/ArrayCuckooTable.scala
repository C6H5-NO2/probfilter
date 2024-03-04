package probfilter.pdsa

import scala.collection.AbstractIterator
import scala.reflect.ClassTag


/**
 * A [[probfilter.pdsa.CuckooTable]] based on [[scala.Array]].
 *
 * @note Mutation will always copy the whole array.
 * @todo Use tree-like structure as [[scala.collection.immutable.Vector]]. Also, need to avoid boxing.
 */
@SerialVersionUID(1L)
final class ArrayCuckooTable[@specialized(Byte, Short, Long) E: ClassTag]
(private val data: Array[E], private val overflow: CuckooTable[E], private val bucketSize: Int, private val arraySize: Int)
  extends CuckooTable[E] {
  def this(numBuckets: Int, bucketSize: Int) =
    this(Array.fill(numBuckets * bucketSize)(0L.asInstanceOf[E]), new MapCuckooTable[E](), bucketSize, 0)

  override def at(i: Int): CuckooBucket[E] = new ArrayCuckooTable.ArrayCuckooBucket(this, i)

  override def size: Int = arraySize + overflow.size
}


object ArrayCuckooTable {
  private final class ArrayCuckooBucket[@specialized(Byte, Short, Long) E: ClassTag]
  (private val table: ArrayCuckooTable[E], val at: Int) extends CuckooBucket[E] {
    private def arrayIterator: Iterator[E] = new ArrayCuckooBucketIterator[E](table.data, at, table.bucketSize)

    override def iterator: Iterator[E] = arrayIterator concat[E] table.overflow.at(at).iterator

    override def add(e: E): CuckooTable[E] = {
      require(0L != e.asInstanceOf[Long])
      var pos = at * table.bucketSize
      val until = pos + table.bucketSize
      while (pos < until) {
        if (0L == table.data.apply(pos).asInstanceOf[Long])
          return new ArrayCuckooTable[E](table.data.updated(pos, e), table.overflow, table.bucketSize, table.arraySize + 1)
        pos += 1
      }
      new ArrayCuckooTable[E](table.data, table.overflow.at(at).add(e), table.bucketSize, table.arraySize)
    }

    override def remove(e: E): CuckooTable[E] = {
      require(0L != e.asInstanceOf[Long])

      val oldOverflowSize = table.overflow.size
      val overflowBucket = table.overflow.at(at)
      val newOverflow = overflowBucket.remove(e)
      if (oldOverflowSize > newOverflow.size)
        return new ArrayCuckooTable[E](table.data, newOverflow, table.bucketSize, table.arraySize)

      var pos = at * table.bucketSize
      val until = pos + table.bucketSize
      while (pos < until) {
        if (e == table.data.apply(pos)) {
          if (overflowBucket.size > 0) {
            val (entry, newOverflow) = overflowBucket.pop()
            val newData = table.data.updated(pos, entry)
            return new ArrayCuckooTable[E](newData, newOverflow, table.bucketSize, table.arraySize)
          }
          else {
            val newData = table.data.updated[E](pos, 0L.asInstanceOf[E])
            return new ArrayCuckooTable[E](newData, table.overflow, table.bucketSize, table.arraySize - 1)
          }
        }
        pos += 1
      }

      table
    }

    override def replace(e: E, victim: Int): (E, CuckooTable[E]) = ???

    override def pop(): (E, CuckooTable[E]) = {
      val overflowBucket = table.overflow.at(at)
      if (overflowBucket.size > 0) {
        val (poppedEntry, newOverflow) = overflowBucket.pop()
        return (poppedEntry, new ArrayCuckooTable[E](table.data, newOverflow, table.bucketSize, table.arraySize))
      }

      var pos = at * table.bucketSize
      val until = pos + table.bucketSize
      while (pos < until) {
        val poppedEntry = table.data.apply(pos)
        if (0L != poppedEntry.asInstanceOf[Long]) {
          val newData = table.data.updated[E](pos, 0L.asInstanceOf[E])
          return (poppedEntry, new ArrayCuckooTable[E](newData, table.overflow, table.bucketSize, table.arraySize - 1))
        }
        pos += 1
      }

      throw new NoSuchElementException()
    }
  }

  private final class ArrayCuckooBucketIterator[@specialized(Byte, Short, Long) E] private
  (private val data: Array[E], private val until: Int) extends AbstractIterator[E] {
    def this(data: Array[E], bucketIdx: Int, bucketSize: Int) = {
      this(data, bucketIdx * bucketSize + bucketSize)
      nextPos = bucketIdx * bucketSize
      advance()
    }

    private var nextPos: Int = 0

    private def advance(): Unit = {
      while (nextPos < until && 0L == data.apply(nextPos).asInstanceOf[Long])
        nextPos += 1
    }

    override def hasNext: Boolean = nextPos < until

    override def next(): E = {
      val pos = nextPos
      advance()
      data.apply(pos)
    }
  }
}
