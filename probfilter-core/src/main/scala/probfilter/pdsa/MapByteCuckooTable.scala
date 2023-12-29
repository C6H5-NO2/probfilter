package probfilter.pdsa

import probfilter.util.UnsignedNumber

import scala.collection.immutable.HashMap


/**
 * A [[probfilter.pdsa.ByteCuckooTable]] based on [[scala.collection.immutable.HashMap]].
 *
 * @todo Provide also a more compact `ArrayByteCuckooTable` based on `Array`
 */
@SerialVersionUID(1L)
final class MapByteCuckooTable private(
  private val data: HashMap[Int, Array[Byte]], @transient private val victimIdx: Int
) extends ByteCuckooTable {
  def this() = this(HashMap.empty, 0)

  def this(data: HashMap[Int, Array[Byte]]) = this(data, 0)

  override def at(i: Int): ByteCuckooBucket = {
    new MapByteCuckooTable.MapByteCuckooBucket1(this, i)
  }

  override def toString: String = data.view.map { case (i, v) =>
    val e = v.view.map(UnsignedNumber.toString).mkString("[", ", ", "]")
    s"$i->$e"
  }.mkString("T{", ", ", "}")
}


object MapByteCuckooTable {
  private final class MapByteCuckooBucket1(private val table: MapByteCuckooTable, private val i: Int)
    extends ByteCuckooBucket {
    override def size(): Int = table.data.get(i).fold(0)(_.length)

    override def contains(fp: Byte): Boolean = table.data.get(i).fold(false)(_.contains(fp))

    override def get(): Array[Byte] = table.data.get(i).fold(Array.emptyByteArray)(_.clone())

    override def add(byte$: Byte): ByteCuckooTable = {
      val newBucket = table.data.get(i).fold(Array.apply(byte$))(_.appended(byte$))
      val newData = table.data.updated(i, newBucket)
      new MapByteCuckooTable(newData, table.victimIdx)
    }

    override def replace(byte$: Byte): (ByteCuckooTable, Byte) = {
      val bucket = table.data.apply(i)
      if (bucket.isEmpty)
        throw new NoSuchElementException()
      val victimIdx = (table.victimIdx + 1) % bucket.length
      val swapped = bucket.apply(victimIdx)
      val newBucket = bucket.updated(victimIdx, byte$)
      val newData = table.data.updated(i, newBucket)
      (new MapByteCuckooTable(newData, victimIdx), swapped)
    }
  }
}
