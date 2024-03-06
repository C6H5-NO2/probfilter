package probfilter.pdsa

import scala.collection.immutable.HashMap
import scala.reflect.ClassTag


/** A [[probfilter.pdsa.CuckooTable]] based on [[scala.collection.immutable.HashMap]]. */
@SerialVersionUID(1L)
final class MapCuckooTable[@specialized(Byte, Short, Long) E: ClassTag](private val data: HashMap[Int, Array[E]], val size: Int) extends CuckooTable[E] {
  def this() = this(HashMap.empty, 0)

  def this(data: HashMap[Int, Array[E]]) = this(data, MapCuckooTable.sizeOf(data))

  override def at(i: Int): CuckooBucket[E] = new MapCuckooTable.MapCuckooBucket(this, i)

  override def toString: String = data.view.map { case (i, v) =>
    val e = v.mkString("[", ", ", "]")
    s"$i->$e"
  }.mkString("T{", ", ", "}")
}


object MapCuckooTable {
  private def sizeOf(data: HashMap[Int, Array[_]]): Int = {
    data.values.foldLeft(0) { (sum, arr) => sum + arr.length }
  }

  private final class MapCuckooBucket[@specialized(Byte, Short, Long) E: ClassTag](private val table: MapCuckooTable[E], val at: Int) extends CuckooBucket[E] {
    override def iterator: Iterator[E] = table.data.get(at).fold(Iterator.empty[E])(_.iterator)

    override def size: Int = table.data.get(at).fold(0)(_.length)

    override def contains(e: E): Boolean = table.data.get(at).fold(false)(_.contains(e))

    override def add(e: E): CuckooTable[E] = {
      val newBucket = table.data.get(at).fold(Array.apply[E](e))(_.appended[E](e))
      val newData = table.data.updated(at, newBucket)
      new MapCuckooTable[E](newData, table.size + 1)
    }

    override def remove(e: E): CuckooTable[E] = table.data.get(at).fold(table) { bucket =>
      val newBucket = bucket.filter(_ != e)
      // todo
      // if (newBucket.length == bucket.length)
      //   throw new NoSuchElementException()
      val newData = if (newBucket.isEmpty) table.data.removed(at) else table.data.updated(at, newBucket)
      new MapCuckooTable[E](newData, table.size - bucket.length + newBucket.length)
    }

    override def replace(e: E, victim: Int): (E, CuckooTable[E]) = {
      val bucket = table.data.apply(at)
      val swappedEntry = bucket.apply(victim)
      val newBucket = bucket.updated(victim, e)
      val newData = table.data.updated(at, newBucket)
      (swappedEntry, new MapCuckooTable[E](newData, table.size))
    }

    override def pop(): (E, CuckooTable[E]) = {
      val bucket = table.data.apply(at)
      val poppedEntry = bucket.apply(bucket.length - 1)
      val newData = if (bucket.length > 1) {
        val newBucket = bucket.take(bucket.length - 1)
        table.data.updated(at, newBucket)
      } else {
        table.data.removed(at)
      }
      (poppedEntry, new MapCuckooTable[E](newData, table.size - 1))
    }
  }
}
