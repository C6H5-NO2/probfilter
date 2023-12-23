package probfilter.crdt

import java.util.function.Predicate


trait CuckooTable extends Serializable {
  def sizeAt(i: Int): Int

  def containsAt(fp: Byte, i: Int): Boolean

  def filterAt(fp: Byte, i: Int): Seq[Long] = filterAt((e: CuckooEntry) => e.fingerprint == fp, i)

  def filterAt(pred: Predicate[CuckooEntry], i: Int): Seq[Long]

  def addAt(e: CuckooEntry, i: Int): CuckooTable

  def replaceAt(e: CuckooEntry, i: Int): (CuckooTable, CuckooEntry)

  def removeIfOnceAt(pred: Predicate[CuckooEntry], i: Int): CuckooTable

  def removeIfAt(pred: Predicate[CuckooEntry], i: Int): CuckooTable

  def iteratorAt(i: Int): Iterator[Long]
}
