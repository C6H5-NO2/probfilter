package probfilter.pdsa.cuckoo

import probfilter.util.FalseRandom

import scala.annotation.tailrec


/**
 * @note This class is for internal use.
 * @see [[probfilter.pdsa.cuckoo.immutable.CuckooFilter]] or [[probfilter.pdsa.cuckoo.mutable.CuckooFilter]]
 */
@SerialVersionUID(1L)
private[cuckoo] final class CuckooFilterOps[E]
(val table: CuckooTableOps, val strategy: CuckooStrategy[E], val mutable: Boolean) extends Serializable {
  private val extractor: FingerprintExtractor = FingerprintExtractor.create(strategy)

  def contains(elem: E): Boolean = {
    val triple = strategy.hashAll(elem)
    val entry = triple.fp
    strategy.storageType() match {
      case EntryStorageType.SIMPLE_BYTE => contains[Byte](triple, entry.asInstanceOf[Byte])
      case EntryStorageType.SIMPLE_SHORT => contains[Short](triple, entry)
      case EntryStorageType.VERSIONED_INT => contains[Int](triple, entry)
      case EntryStorageType.VERSIONED_LONG => contains[Long](triple, entry)
    }
  }

  private def contains[T](triple: CuckooStrategy.Triple, entry: T): Boolean = {
    val data = table.typed[T]
    data.contains(triple.i, entry) || data.contains(triple.j, entry)
  }

  def exists[T](triple: CuckooStrategy.Triple, p: T => Boolean): Boolean = {
    val data = table.typed[T]
    data.exists(triple.i, p) || data.exists(triple.j, p)
  }

  def add(elem: E): CuckooTableOps = {
    val triple = strategy.hashAll(elem)
    val entry = triple.fp
    strategy.storageType() match {
      case EntryStorageType.SIMPLE_BYTE => add[Byte](triple, entry.asInstanceOf[Byte], elem)
      case EntryStorageType.SIMPLE_SHORT => add[Short](triple, entry, elem)
      case EntryStorageType.VERSIONED_INT => add[Int](triple, entry, elem)
      case EntryStorageType.VERSIONED_LONG => add[Long](triple, entry, elem)
    }
  }

  def add[T](triple: CuckooStrategy.Triple, entry: T, elem: Any): TypedCuckooTableOps[T] = {
    val data = table.typed[T]
    val i1 = triple.i
    val i2 = triple.j
    val s1 = data.size(i1)
    val s2 = data.size(i2)
    val c = strategy.bucketSize()
    if (s1 > c)
      throw new CuckooStrategy.BucketOverflowException(elem, i1)
    else if (s2 > c)
      throw new CuckooStrategy.BucketOverflowException(elem, i2)
    else if (s1 < c)
      data.add(i1, entry)
    else if (s2 < c)
      data.add(i2, entry)
    else
      displace(data, new CuckooStrategy.Pair(triple.i, triple.fp), entry, 0, Vector.empty[Int], elem)
  }

  @tailrec
  private def displace[T](data: TypedCuckooTableOps[T],
                          pair: CuckooStrategy.Pair, entry: T, attempts: Int, hist: Vector[Int],
                          elem: Any): TypedCuckooTableOps[T] = {
    if (attempts >= strategy.maxIterations()) {
      rollback(data, pair, entry, hist)
      throw new CuckooStrategy.MaxIterationReachedException(elem, strategy.maxIterations())
    }
    val i1 = pair.i
    val s1 = data.size(i1)
    val c = strategy.bucketSize()
    if (s1 > c) {
      rollback(data, pair, entry, hist)
      throw new CuckooStrategy.BucketOverflowException(elem, i1)
    } else if (s1 < c) {
      data.add(i1, entry)
    } else {
      val victimIndex = FalseRandom.next(s1)
      val tup = data.replace(i1, entry, victimIndex)
      val displacedEntry = tup._1
      val newData = tup._2
      val fp = extractor.extract(displacedEntry)
      val i2 = strategy.altIndexOf(i1, fp)
      val newHist = if (mutable) hist.appended(victimIndex) else hist
      displace(newData, new CuckooStrategy.Pair(i2, fp), displacedEntry, attempts + 1, newHist, elem)
    }
  }

  /** @note No-Op for `!mutable` */
  private def rollback[T](data: TypedCuckooTableOps[T], pair: CuckooStrategy.Pair, entry: T, hist: Vector[Int]): Unit = {
    if (!mutable)
      return
    var currIdx = strategy.altIndexOf(pair.i, pair.fp)
    var currEntry = entry
    hist.reverseIterator.foreach { victimIndex =>
      val tup = data.replace(currIdx, currEntry, victimIndex)
      val displacedEntry = tup._1
      val fp = extractor.extract(displacedEntry)
      currIdx = strategy.altIndexOf(currIdx, fp)
      currEntry = displacedEntry
    }
  }

  def remove(elem: E): CuckooTableOps = {
    val triple = strategy.hashAll(elem)
    strategy.storageType() match {
      case EntryStorageType.SIMPLE_BYTE => remove[Byte](triple, elem)
      case EntryStorageType.SIMPLE_SHORT => remove[Short](triple, elem)
      case EntryStorageType.VERSIONED_INT => remove[Int](triple, elem)
      case EntryStorageType.VERSIONED_LONG => remove[Long](triple, elem)
    }
  }

  private def remove[T](triple: CuckooStrategy.Triple, elem: Any): TypedCuckooTableOps[T] = {
    val data = table.typed[T]
    val a1 = data.get(triple.i).filter(extractor.extract(_) == triple.fp)
    val a2 = data.get(triple.j).filter(extractor.extract(_) == triple.fp)
    val len = a1.length + a2.length
    if (len == 0)
      return data
    val rand = FalseRandom.next(len)
    if (rand < a1.length) {
      val entry = a1.apply(rand)
      data.remove(triple.i, entry)
    } else {
      val entry = a2.apply(rand - a1.length)
      data.remove(triple.j, entry)
    }
  }
}
