package probfilter.crdt

import java.util.function.Predicate
import scala.collection.immutable.HashMap


@SerialVersionUID(1L)
class CuckooTable private(private val data: HashMap[Int, Vector[Long]], @transient private val victimIdx: Int)
  extends Serializable {
  def this() = this(HashMap.empty, 0)

  def sizeAt(i: Int): Int = data.get(i) match {
    case Some(bucket) => bucket.size
    case None => 0
  }

  def containsAt(fp: Byte, i: Int): Boolean = data.get(i) match {
    case Some(bucket) => bucket.exists(long => CuckooEntry.fromLong(long).fingerprint == fp)
    case None => false
  }

  def swapAt(e: CuckooEntry, i: Int): (CuckooTable, CuckooEntry) = {
    val bucket = data(i)
    if (bucket.isEmpty)
      throw new NoSuchElementException()
    val newIdx = (this.victimIdx + 1) % bucket.length
    val oldEntry = CuckooEntry.fromLong(bucket(newIdx))
    val newBucket = bucket.updated(newIdx, e.toLong)
    (new CuckooTable(data.updated(i, newBucket), newIdx), oldEntry)
  }

  def addAt(e: CuckooEntry, i: Int): CuckooTable = {
    val newBucket = data.get(i) match {
      case Some(bucket) => bucket.prepended(e.toLong)
      case None => Vector(e.toLong)
    }
    new CuckooTable(data.updated(i, newBucket), this.victimIdx)
  }

  def removeOnceIfAt(pred: Predicate[CuckooEntry], i: Int): CuckooTable = data.get(i) match {
    case Some(bucket) => {
      val idx = bucket.indexWhere(long => pred.test(CuckooEntry.fromLong(long)))
      if (idx >= 0) {
        val newData =
          if (bucket.size == 1) {
            data.removed(i)
          } else {
            data.updated(i, bucket.take(idx) concat bucket.drop(idx + 1))
          }
        new CuckooTable(newData, this.victimIdx)
      } else {
        this
      }
    }
    case None => this
  }

  def removeIfAt(pred: Predicate[CuckooEntry], i: Int): CuckooTable = data.get(i) match {
    case Some(bucket) => {
      val newBucket = bucket.filter(long => !pred.test(CuckooEntry.fromLong(long)))
      val newData = if (newBucket.isEmpty) data.removed(i) else data.updated(i, newBucket)
      new CuckooTable(newData, this.victimIdx)
    }
    case None => this
  }

  override def toString: String = data.view.mapValues(_.map(CuckooEntry.fromLong)).toMap.toString
}
