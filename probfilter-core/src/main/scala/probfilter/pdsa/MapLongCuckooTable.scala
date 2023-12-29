package probfilter.pdsa

import java.util.stream.LongStream
import scala.collection.immutable.HashMap


/**
 * A [[probfilter.pdsa.LongCuckooTable]] based on [[scala.collection.immutable.HashMap]].
 *
 * @todo Provide also a more compact `ArrayLongCuckooTable` based on `Array[Long]`
 */
@SerialVersionUID(1L)
final class MapLongCuckooTable private(
  private val data: HashMap[Int, Array[Long]], @transient private val victimIdx: Int
) extends LongCuckooTable {
  def this() = this(HashMap.empty, 0)

  def this(data: HashMap[Int, Array[Long]]) = this(data, 0)

  override def at(i: Int): LongCuckooBucket = {
    new MapLongCuckooTable.MapLongCuckooBucket1(this, i)
  }

  override def toString: String = data.view.map { case (i, v) =>
    val e = v.view.map(CuckooEntry.of).mkString("[", ", ", "]")
    s"$i->$e"
  }.mkString("T{", ", ", "}")
}


object MapLongCuckooTable {
  private final class MapLongCuckooBucket1(private val table: MapLongCuckooTable, private val i: Int)
    extends LongCuckooBucket {
    override def size(): Int = {
      table.data.get(i).fold(0)(_.distinctBy(CuckooEntry.of(_).fingerprint).length)
    }

    override def contains(fp: Byte): Boolean = {
      table.data.get(i).fold(false)(_.exists(CuckooEntry.of(_).fingerprint == fp))
    }

    override def get(fp: Byte): Array[Long] = {
      table.data.get(i).fold(Array.emptyLongArray)(_.filter(CuckooEntry.of(_).fingerprint == fp))
    }

    override def stream(): LongStream = {
      table.data.get(i).fold(LongStream.empty())(LongStream.of(_: _*))
    }

    override def add(long$: Long): LongCuckooTable = {
      val newBucket = table.data.get(i).fold(Array.apply(long$))(_.appended(long$).sorted)
      val newData = table.data.updated(i, newBucket)
      new MapLongCuckooTable(newData, table.victimIdx)
    }

    override def add(longs: Array[Long]): LongCuckooTable = {
      val newBucket = table.data.get(i).fold(longs.sorted)(_.appendedAll(longs).sorted)
      val newData = table.data.updated(i, newBucket)
      new MapLongCuckooTable(newData, table.victimIdx)
    }

    override def remove(fp: Byte): LongCuckooTable = table.data.get(i).fold(table) { bucket =>
      val newBucket = bucket.filter(CuckooEntry.of(_).fingerprint != fp)
      val newData = if (newBucket.isEmpty) table.data.removed(i) else table.data.updated(i, newBucket)
      new MapLongCuckooTable(newData, table.victimIdx)
    }

    override def replace(longs: Array[Long]): (LongCuckooTable, Array[Long]) = {
      require(longs.nonEmpty)
      val bucket = table.data.apply(i)
      if (bucket.isEmpty)
        throw new NoSuchElementException()
      // The size is small. Keep it linear.
      val fps = bucket.distinctBy(CuckooEntry.of(_).fingerprint)
      val victimIdx = (table.victimIdx + 1) % fps.length
      val victimFp = CuckooEntry.of(fps.apply(victimIdx)).fingerprint
      val (swapped, kept) = bucket.partition(CuckooEntry.of(_).fingerprint == victimFp)
      val newBucket = kept.appendedAll(longs).sorted
      val newData = table.data.updated(i, newBucket)
      (new MapLongCuckooTable(newData, victimIdx), swapped)
    }
  }
}
