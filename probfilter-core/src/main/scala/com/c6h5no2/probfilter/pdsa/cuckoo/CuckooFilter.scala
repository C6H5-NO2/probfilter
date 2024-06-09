package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.pdsa.Filter
import com.c6h5no2.probfilter.util.{RandomIntGenerator, SimpleLCG, Immutable => ImmCol, Mutable => MutCol}

import scala.annotation.tailrec
import scala.util.Try


/** A [[https://doi.org/10.1145/2674005.2674994 cuckoo filter]]. */
sealed trait CuckooFilter[E] extends Filter[E, CuckooFilter[E]] {
  override final def size: Int = table.typed.size

  override final def capacity: Int = strategy.capacity

  override final def fpp: Double = strategy.fpp

  override final def contains(elem: E): Boolean = {
    val triple = strategy.hashAll(elem)
    exists(triple, entry => strategy.entryType.matchFp(entry, triple.fp))
  }

  private def exists[T](triple: CuckooStrategy.Triple, predicate: T => Boolean): Boolean = {
    val ttable = table.typed[T]
    ttable.exists(triple.i, predicate) || ttable.exists(triple.j, predicate)
  }

  /** @see [[TypedCuckooTableOps.contains]] */
  final def contains[T](index: Int, entry: T): Boolean = {
    val ttable = table.typed[T]
    ttable.contains(index, entry)
  }

  /**
   * @inheritdoc
   * @throws CuckooStrategy.MaxIterationReachedException if max iteration is reached
   * @throws scala.MatchError unless `strategy.entryType` is `CuckooEntryType.SIMPLE_*`
   */
  override final def add(elem: E): CuckooFilter[E] = {
    (strategy.entryType: @unchecked) match {
      case CuckooEntryType.SIMPLE_BYTE => add[Byte](elem, _.asInstanceOf[Byte])
      case CuckooEntryType.SIMPLE_SHORT => add[Short](elem, s => s)
    }
  }

  /**
   * @param entryBuilder a single-use [[scala.Function1]] mapping a fingerprint to the entry to store
   * @note `strategy.entryType.extractFp(entryBuilder.apply(fp))` <i>must</i> equal to `fp`.
   * @throws CuckooStrategy.MaxIterationReachedException if max iteration is reached
   */
  final def add[T](elem: E, entryBuilder: Short => T): CuckooFilter[E] = {
    val triple = strategy.hashAll(elem)
    val entry = entryBuilder.apply(triple.fp)
    val context = new CuckooFilter.InsertContext[T](this)
    val bucketIndex1 = triple.i
    val bucketIndex2 = triple.j
    val bucketSize1 = context.ttable.size(bucketIndex1)
    val bucketSize2 = context.ttable.size(bucketIndex2)
    val bucketCapacity = strategy.bucketSize
    val index =
      if (bucketSize1 <= bucketCapacity && bucketSize2 > bucketCapacity) bucketIndex1
      else if (bucketSize1 > bucketCapacity && bucketSize2 <= bucketCapacity) bucketIndex2
      else {if (context.rng.nextInt(2) == 0) bucketIndex1 else bucketIndex2}
    val res = insert(context, new CuckooStrategy.Pair(index, triple.fp), entry, elem)
    copy(res.ttable, res.rng)
  }

  /** @throws CuckooStrategy.MaxIterationReachedException . */
  @tailrec
  private def insert[T](
    context: CuckooFilter.InsertContext[T],
    pair: CuckooStrategy.Pair,
    entry: T,
    elem: Any,
  ): CuckooFilter.InsertContext[T] = {
    if (context.quota < 0) {
      rollback(context, pair, entry)
      throw new CuckooStrategy.MaxIterationReachedException(elem, strategy.maxIterations)
    }
    val bucketIndex = pair.i
    val bucketSize = context.ttable.size(bucketIndex)
    val bucketCapacity = strategy.bucketSize
    if (bucketSize < bucketCapacity) {
      // append
      val newTTable = context.ttable.add(bucketIndex, entry)
      context.copy(newTTable)
    } else if (bucketSize == bucketCapacity) {
      // displace
      val victimIndex = context.rng.nextInt(bucketSize)
      val tup = context.ttable.replace(bucketIndex, entry, victimIndex)
      val victimEntry = tup._1
      val newTTable = tup._2
      val victimFp = strategy.entryType.extractFp(victimEntry)
      val altIndex = strategy.altIndexOf(bucketIndex, victimFp)
      val newContext = context.next(newTTable, victimIndex)
      val newPair = new CuckooStrategy.Pair(altIndex, victimFp)
      insert(newContext, newPair, victimEntry, elem)
    } else {
      // evict
      val victimIndex = context.rng.nextInt(bucketSize)
      val victimEntry = context.ttable.get(bucketIndex).apply(victimIndex)
      val victimFp = strategy.entryType.extractFp(victimEntry)
      val altIndex = strategy.altIndexOf(bucketIndex, victimFp)
      val newContext =
        if (altIndex == bucketIndex) {
          // retry
          context.next()
        } else {
          // rebalance
          val evictedTTable = context.ttable.remove(bucketIndex, victimEntry)
          val evictedContext = context.next(evictedTTable, victimIndex)
          val evictedPair = new CuckooStrategy.Pair(altIndex, victimFp)
          val res = tryInsert(evictedContext, evictedPair, victimEntry, elem)
          res.getOrElse(context.last())
        }
      insert(newContext, pair, entry, elem)
    }
  }

  /** A non-tail [[CuckooFilter.insert]]. */
  private def tryInsert[T](
    context: CuckooFilter.InsertContext[T],
    pair: CuckooStrategy.Pair,
    entry: T,
    elem: Any,
  ): Try[CuckooFilter.InsertContext[T]] = {
    Try.apply(insert(context, pair, entry, elem))
  }

  /** @note Only the mutable variant needs this. It is effectively a no-op for immutable variant. */
  protected def rollback[T](
    context: CuckooFilter.InsertContext[T],
    pair: CuckooStrategy.Pair,
    entry: T,
  ): Unit = {
    var currIndex = strategy.altIndexOf(pair.i, pair.fp)
    var currEntry = entry
    context.hist.foreach { victimIndex =>
      val tup = context.ttable.replace(currIndex, currEntry, victimIndex)
      val displacedEntry = tup._1
      val fp = strategy.entryType.extractFp(displacedEntry)
      currIndex = strategy.altIndexOf(currIndex, fp)
      currEntry = displacedEntry
    }
  }

  override final def remove(elem: E): CuckooFilter[E] = {
    val triple = strategy.hashAll(elem)
    val rng = rngCopy
    val ttable = remove(triple, rng)
    copy(ttable, rng)
  }

  private def remove[T](triple: CuckooStrategy.Triple, rng: RandomIntGenerator): TypedCuckooTable[T] = {
    val ttable = table.typed[T]
    val bucket1 = ttable.get(triple.i).filter(entry => strategy.entryType.matchFp(entry, triple.fp))
    val bucket2 = ttable.get(triple.j).filter(entry => strategy.entryType.matchFp(entry, triple.fp))
    val length = bucket1.length + bucket2.length
    if (length == 0) {
      return ttable
    }
    val index = rng.nextInt(length)
    if (index < bucket1.length) {
      val entry = bucket1.apply(index)
      ttable.remove(triple.i, entry)
    } else {
      val entry = bucket2.apply(index - bucket1.length)
      ttable.remove(triple.j, entry)
    }
  }

  /**
   * @note `op` is applied iteratively from left to right, so please be mindful of the order of reads/writes.
   * @see [[TypedCuckooTableOps.zipFold]]
   */
  final def zipFold[T](that: CuckooFilter[E], z: TypedCuckooTable[T])
                      (op: CuckooFilter.ZipFoldOp[T]): CuckooFilter[E] = {
    val thisTTable = this.table.typed[T]
    val thatTTable = that.table.typed[T]
    val maxBuckets = math.max(thisTTable.numBuckets, thatTTable.numBuckets)
    val newTTable = thisTTable.zipFold(thatTTable)(z.reserve(maxBuckets))(op)
    copy(newTTable, rngCopy)
  }

  /** [[CuckooFilter.zipFold]] with starting value equal to `z.table.typed[T]` */
  final def zipFold[T](that: CuckooFilter[E], z: CuckooFilter[E])
                      (op: CuckooFilter.ZipFoldOp[T]): CuckooFilter[E] = {
    zipFold(that, z.table.typed[T])(op)
  }

  /** [[CuckooFilter.zipFold]] with starting value being empty */
  def zipFold[T](that: CuckooFilter[E])(op: CuckooFilter.ZipFoldOp[T]): CuckooFilter[E]

  @deprecated("not very effective")
  final def rebalance(repeat: Int): CuckooFilter[E] = {
    Range.apply(0, repeat).foldLeft(this) { (cf, _) =>
      val rng = cf.rngCopy
      val ttable = cf.rebalance(rng)
      cf.copy(ttable, rng)
    }
  }

  @deprecated
  private def rebalance[T](rng: RandomIntGenerator): TypedCuckooTable[T] = {
    Range.apply(0, strategy.numBuckets).foldLeft(table.typed[T]) { (ttable, _) =>
      val bucketIndex = rng.nextInt(strategy.numBuckets)
      if (ttable.size(bucketIndex) <= strategy.bucketSize) {
        ttable
      } else {
        ttable.get(bucketIndex).foldLeft(ttable) { (ttable, victimEntry) =>
          if (ttable.size(bucketIndex) <= strategy.bucketSize) {
            ttable
          } else {
            val altIndex = strategy.altIndexOf(bucketIndex, strategy.entryType.extractFp(victimEntry))
            if (altIndex == bucketIndex || ttable.size(altIndex) >= strategy.bucketSize) {
              ttable
            } else {
              ttable.remove(bucketIndex, victimEntry).add(altIndex, victimEntry)
            }
          }
        }
      }
    }
  }

  def strategy: CuckooStrategy[E]

  protected def table: CuckooTable

  /**
   * @return a <i>copy</i> of the RNG state as a new instance
   * @note Always make a copy of the RNG.
   */
  protected def rngCopy: RandomIntGenerator

  protected def copy(table: CuckooTable, rng: RandomIntGenerator): CuckooFilter[E]

  override def toString: String = s"${getClass.getName}($table)"
}

object CuckooFilter {
  def apply[E](mutable: Boolean, strategy: CuckooStrategy[E], seed: Int): CuckooFilter[E] = {
    if (mutable)
      new CuckooFilter.Mutable[E](strategy, seed)
    else
      new CuckooFilter.Immutable[E](strategy, seed)
  }

  @SerialVersionUID(1L)
  final class Immutable[E] private(
    protected val table: CuckooTable,
    val strategy: CuckooStrategy[E],
    rng: RandomIntGenerator,
  ) extends CuckooFilter[E]
    with ImmCol {
    def this(strategy: CuckooStrategy[E], seed: Int) =
      this(CuckooFilter.Immutable.emptyTable(strategy), strategy, new SimpleLCG(seed))

    override protected def rollback[T](context: InsertContext[T], pair: CuckooStrategy.Pair, entry: T): Unit = {}

    override def zipFold[T](that: CuckooFilter[E])(op: CuckooFilter.ZipFoldOp[T]): CuckooFilter[E] = {
      zipFold(that, CuckooFilter.Immutable.emptyTable(strategy).typed[T])(op)
    }

    override protected def rngCopy: RandomIntGenerator = rng.copy()

    override protected def copy(table: CuckooTable, rng: RandomIntGenerator): CuckooFilter[E] = {
      new CuckooFilter.Immutable[E](table, this.strategy, rng.copy())
    }
  }

  object Immutable {
    private def emptyTable(strategy: CuckooStrategy[_]): CuckooTable =
      new MapCuckooTable.Immutable(strategy.bucketSize)(strategy.entryType.storageType)
  }

  @SerialVersionUID(1L)
  final class Mutable[E] private(
    protected var table: CuckooTable,
    val strategy: CuckooStrategy[E],
    private[this] var rng: RandomIntGenerator,
  ) extends CuckooFilter[E]
    with MutCol {
    def this(strategy: CuckooStrategy[E], seed: Int) =
      this(CuckooFilter.Mutable.emptyTable(strategy), strategy, new SimpleLCG(seed))

    override def zipFold[T](that: CuckooFilter[E])(op: CuckooFilter.ZipFoldOp[T]): CuckooFilter[E] = {
      zipFold(that, CuckooFilter.Mutable.emptyTable(strategy).typed[T])(op)
    }

    override protected def rngCopy: RandomIntGenerator = rng.copy()

    override protected def copy(table: CuckooTable, rng: RandomIntGenerator): CuckooFilter[E] = {
      this.table = table
      this.rng = rng
      this
    }
  }

  object Mutable {
    private def emptyTable(strategy: CuckooStrategy[_]): CuckooTable =
      new ArrayCuckooTable.Mutable(strategy.numBuckets, strategy.bucketSize)(strategy.entryType.storageType)
  }

  /**
   * @param rng a copy of the current RNG state
   * @param hist a stack of operation history, i.e. the indexes of victim entries within their buckets
   * @param quota the number of remaining available iterations
   */
  private final class InsertContext[T] private(
    val ttable: TypedCuckooTable[T],
    val rng: RandomIntGenerator,
    val hist: Seq[Int],
    val quota: Int,
  ) {
    def this(cf: CuckooFilter[_]) =
      this(cf.table.typed[T], cf.rngCopy, List.empty[Int], cf.strategy.maxIterations)

    def copy(ttable: TypedCuckooTable[T]): InsertContext[T] =
      new InsertContext[T](ttable, this.rng, this.hist, this.quota)

    def next(): InsertContext[T] =
      new InsertContext[T](this.ttable, this.rng, this.hist, this.quota - 1)

    def next(ttable: TypedCuckooTable[T], victimIndex: Int): InsertContext[T] =
      new InsertContext[T](ttable, this.rng, this.hist.prepended(victimIndex), this.quota - 1)

    def last(): InsertContext[T] =
      new InsertContext[T](this.ttable, this.rng, this.hist, -1)
  }

  private final type ZipFoldOp[T] = (TypedCuckooTable[T], Array[T], Array[T], Int) => TypedCuckooTable[T]
}
