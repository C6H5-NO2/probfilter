package probfilter.crdt

import scala.collection.immutable.HashMap


class CuckooTable(val data: HashMap[Int, CuckooBucket]) extends Serializable {
  def get(i: Int): CuckooBucket = data.getOrElse(i, ???)

  def atContains(i: Int, fp: Byte): Boolean = data.get(i) match {
    case Some(bucket) => bucket.data.exists(CuckooEntry.fromLong(_).fingerprint == fp)
    case None => false
  }

  def atRemove(i: Int, fp: Byte): CuckooTable = data.get(i) match {
    case Some(bucket) => {
      val bucketData = bucket.data.filter(CuckooEntry.fromLong(_).fingerprint != fp)
      val tableData = data.updated(i, new CuckooBucket(bucketData))
      new CuckooTable(tableData)
    }
    case None => this
  }
}
