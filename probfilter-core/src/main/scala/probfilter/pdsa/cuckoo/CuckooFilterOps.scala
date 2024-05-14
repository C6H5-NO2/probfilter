package probfilter.pdsa.cuckoo

import probfilter.util.SimpleLCG

import scala.annotation.tailrec
import scala.reflect.ClassTag


/**
 * @note This class is for internal use.
 * @see [[probfilter.pdsa.cuckoo.immutable.CuckooFilter]] or [[probfilter.pdsa.cuckoo.mutable.CuckooFilter]]
 */
@SerialVersionUID(1L)
private[cuckoo] final class CuckooFilterOps[E] private(
  private val mutable: Boolean, val table: CuckooTableOps,
  val strategy: CuckooStrategy[E], private val extractor: FingerprintExtractor,
  private val rnd: SimpleLCG
) extends Serializable {
  def this(mutable: Boolean, empty: ClassTag[_] => CuckooTableOps, strategy: CuckooStrategy[E], seed: Int) = this(
    mutable,
    strategy.storageType() match {
      case EntryStorageType.SIMPLE_BYTE => empty(ClassTag.Byte)
      case EntryStorageType.SIMPLE_SHORT => empty(ClassTag.Short)
      case EntryStorageType.VERSIONED_INT => empty(ClassTag.Int)
      case EntryStorageType.VERSIONED_LONG => empty(ClassTag.Long)
    },
    strategy,
    FingerprintExtractor.create(strategy),
    new SimpleLCG(seed)
  )

  def contains(elem: E): Boolean = {
    val triple = strategy.hashAll(elem)
    strategy.storageType() match {
      case EntryStorageType.SIMPLE_BYTE => contains[Byte](triple, triple.fp.asInstanceOf[Byte])
      case EntryStorageType.SIMPLE_SHORT => contains[Short](triple, triple.fp)
      case EntryStorageType.VERSIONED_INT => exists[Int](triple, extractor.extract(_) == triple.fp)
      case EntryStorageType.VERSIONED_LONG => exists[Long](triple, extractor.extract(_) == triple.fp)
    }
  }

  private def contains[T](triple: CuckooStrategy.Triple, entry: T): Boolean = {
    val data = table.typed[T]
    data.contains(triple.i, entry) || data.contains(triple.j, entry)
  }

  private def exists[T](triple: CuckooStrategy.Triple, p: T => Boolean): Boolean = {
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
      val victimIndex = rnd.next(s1)
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
    val rand = rnd.next(len)
    if (rand < a1.length) {
      val entry = a1.apply(rand)
      data.remove(triple.i, entry)
    } else {
      val entry = a2.apply(rand - a1.length)
      data.remove(triple.j, entry)
    }
  }

  def rebalance(): CuckooTableOps = {
    strategy.storageType() match {
      case EntryStorageType.SIMPLE_BYTE => rebalanceImpl[Byte]()
      case EntryStorageType.SIMPLE_SHORT => rebalanceImpl[Short]()
      case EntryStorageType.VERSIONED_INT => rebalanceImpl[Int]()
      case EntryStorageType.VERSIONED_LONG => rebalanceImpl[Long]()
    }
  }

  private def rebalanceImpl[T](): TypedCuckooTableOps[T] = {
    val data = table.typed[T]
    Range.apply(0, strategy.numBuckets()).foldLeft(data) { (data, index) =>
      if (data.size(index) <= strategy.bucketSize()) {
        data
      } else {
        data.get(index).foldLeft(data) { (data, entry) =>
          if (data.size(index) <= strategy.bucketSize()) {
            data
          } else {
            val altIndex = strategy.altIndexOf(index, extractor.extract(entry))
            if (altIndex == index || data.size(altIndex) >= strategy.bucketSize()) {
              data
            } else {
              data.remove(index, entry).add(altIndex, entry)
            }
          }
        }
      }
    }
  }

  def copy(table: CuckooTableOps): CuckooFilterOps[E] = new CuckooFilterOps[E](mutable, table, strategy, extractor, rnd.copy())
}
