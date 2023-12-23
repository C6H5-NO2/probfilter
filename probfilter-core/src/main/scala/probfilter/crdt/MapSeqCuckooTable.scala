package probfilter.crdt

import java.util.function.Predicate
import scala.collection.immutable.HashMap
import scala.collection.mutable


@SerialVersionUID(1L)
class MapSeqCuckooTable private(private val data: HashMap[Int, Vector[Long]], @transient private val victimIdx: Int)
  extends CuckooTable {
  def this() = this(HashMap.empty, 0)

  def this(data: mutable.HashMap[Int, mutable.ArrayBuffer[Long]]) =
    this(data.view.mapValues(_.toVector).to(HashMap), 0)

  override def sizeAt(i: Int): Int = data.get(i) match {
    case Some(bucket) => bucket.size
    case None => 0
  }

  override def containsAt(fp: Byte, i: Int): Boolean = data.get(i) match {
    case Some(bucket) => bucket.exists(CuckooEntry.fpEq(_, fp))
    case None => false
  }

  override def filterAt(pred: Predicate[CuckooEntry], i: Int): Seq[Long] = data.get(i) match {
    case Some(bucket) => bucket.filter(long => pred.test(CuckooEntry.fromLong(long)))
    case None => Vector.empty
  }

  override def addAt(e: CuckooEntry, i: Int): MapSeqCuckooTable = {
    val newBucket = data.get(i) match {
      case Some(bucket) => bucket.prepended(e.toLong)
      case None => Vector(e.toLong)
    }
    new MapSeqCuckooTable(data.updated(i, newBucket), this.victimIdx)
    // todo: optionally switch to more compact implementation based on the load
  }

  override def replaceAt(e: CuckooEntry, i: Int): (MapSeqCuckooTable, CuckooEntry) = {
    val bucket = data(i)
    if (bucket.isEmpty)
      throw new NoSuchElementException()
    val newIdx = (this.victimIdx + 1) % bucket.length
    val oldEntry = CuckooEntry.fromLong(bucket(newIdx))
    val newBucket = bucket.updated(newIdx, e.toLong)
    (new MapSeqCuckooTable(data.updated(i, newBucket), newIdx), oldEntry)
  }

  override def removeIfOnceAt(pred: Predicate[CuckooEntry], i: Int): MapSeqCuckooTable = data.get(i) match {
    case Some(bucket) => {
      val idx = bucket.indexWhere(long => pred.test(CuckooEntry.fromLong(long)))
      if (idx >= 0) {
        val newData =
          if (bucket.size == 1) {
            data.removed(i)
          } else {
            data.updated(i, bucket.take(idx) concat bucket.drop(idx + 1))
          }
        new MapSeqCuckooTable(newData, this.victimIdx)
      } else {
        this
      }
    }
    case None => this
  }

  override def removeIfAt(pred: Predicate[CuckooEntry], i: Int): MapSeqCuckooTable = data.get(i) match {
    case Some(bucket) => {
      val newBucket = bucket.filter(long => !pred.test(CuckooEntry.fromLong(long)))
      val newData = if (newBucket.isEmpty) data.removed(i) else data.updated(i, newBucket)
      new MapSeqCuckooTable(newData, this.victimIdx)
    }
    case None => this
  }

  override def iteratorAt(i: Int): Iterator[Long] = this.data.getOrElse(i, Vector.empty).iterator

  override def toString: String = data.view.map { case (i, v) =>
    val e = v.view.map(CuckooEntry.fromLong).mkString("[", ", ", "]")
    s"$i->$e"
  }.mkString("T{", ", ", "}")
}
