package probfilter.pdsa.cuckoo

import probfilter.pdsa.cuckoo.CuckooFilterOps.InsContext
import probfilter.util.SimpleLCG

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.Try


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

  /**
   * @param triple index hash and fingerprint hash
   * @param entry the entry to be inserted
   * @param elem the original element; only for the purpose of logging
   * @note The three parameters should match, i.e. pair &lt;- entry &lt;- elem.
   */
  def add[T](triple: CuckooStrategy.Triple, entry: T, elem: Any): TypedCuckooTableOps[T] = {
    val data = table.typed[T]
    val i1 = triple.i
    val i2 = triple.j
    val s1 = data.size(i1)
    val s2 = data.size(i2)
    val c = strategy.bucketSize()
    val index =
      if (s1 <= c && s2 > c) i1
      else if (s1 > c && s2 <= c) i2
      else {if (rnd.next(2) == 0) i1 else i2}
    val context = new InsContext[T](data, strategy)
    val res = insert(context, new CuckooStrategy.Pair(index, triple.fp), entry, elem)
    res.ttable
  }

  @tailrec
  private def insert[T](context: InsContext[T], pair: CuckooStrategy.Pair, entry: T, elem: Any): InsContext[T] = {
    if (context.quota < 0) {
      rollback(context.ttable, context.hist, pair, entry)
      throw new CuckooStrategy.MaxIterationReachedException(elem, strategy.maxIterations())
    }
    val index = pair.i
    val size = context.ttable.size(index)
    if (size < strategy.bucketSize()) {
      // append
      val newTTable = context.ttable.add(index, entry)
      context.copy(newTTable)
    } else if (size == strategy.bucketSize()) {
      // displace
      val victimIndex = rnd.next(size)
      val tup = context.ttable.replace(index, entry, victimIndex)
      val victimEntry = tup._1
      val newTTable = tup._2
      val victimFp = extractor.extract(victimEntry)
      val altIndex = strategy.altIndexOf(index, victimFp)
      val newHist = if (mutable) context.hist.appended(victimIndex) else context.hist
      val newContext = context.next(newTTable, newHist)
      insert(newContext, new CuckooStrategy.Pair(altIndex, victimFp), victimEntry, elem)
    } else {
      // evict - rebalance
      val bucket = context.ttable.get(index)
      val victimIndex = rnd.next(size)
      val victimEntry = bucket.apply(victimIndex)
      val victimFp = extractor.extract(victimEntry)
      val altIndex = strategy.altIndexOf(index, victimFp)
      if (altIndex == index) {
        // retry
        val newContext = context.next()
        insert(newContext, pair, entry, elem)
      } else {
        val evictedTTable = context.ttable.remove(index, victimEntry)
        val evictedHist = if (mutable) context.hist.appended(victimIndex) else context.hist
        val evictedContext = context.next(evictedTTable, evictedHist)
        val res = tryInsert(evictedContext, new CuckooStrategy.Pair(altIndex, victimFp), victimEntry, elem)
        val newContext = res.getOrElse(context.last())
        insert(newContext, pair, entry, elem)
      }
    }
  }

  /** A non-tail [[probfilter.pdsa.cuckoo.CuckooFilterOps.insert]]. */
  private def tryInsert[T](context: InsContext[T], pair: CuckooStrategy.Pair, entry: T, elem: Any): Try[InsContext[T]] = {
    Try.apply(insert(context, pair, entry, elem))
  }

  /** @note No-Op for `!mutable` */
  private def rollback[T](data: TypedCuckooTableOps[T], hist: Vector[Int], pair: CuckooStrategy.Pair, entry: T): Unit = {
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
    Range.apply(0, strategy.numBuckets()).foldLeft(data) { (data, _) =>
      val index = rnd.next(strategy.numBuckets())
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


private[cuckoo] object CuckooFilterOps {
  /**
   * @param hist operation history containing the indexes of victim entries within their buckets
   * @param quota the number of remaining available iterations
   */
  private class InsContext[T] private(val ttable: TypedCuckooTableOps[T], val hist: Vector[Int], val quota: Int) {
    def this(ttable: TypedCuckooTableOps[T], strategy: CuckooStrategy[_]) = this(ttable, Vector.empty[Int], strategy.maxIterations())

    def copy(ttable: TypedCuckooTableOps[T]): InsContext[T] = new InsContext[T](ttable, hist, quota)

    def next(): InsContext[T] = new InsContext[T](ttable, hist, quota - 1)

    def next(ttable: TypedCuckooTableOps[T], hist: Vector[Int]): InsContext[T] = new InsContext[T](ttable, hist, quota - 1)

    def last(): InsContext[T] = new InsContext[T](ttable, hist, -1)
  }
}
