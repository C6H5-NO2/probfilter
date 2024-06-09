package probfilter.pdsa.cuckoo.mutable

import probfilter.pdsa.Filter
import probfilter.pdsa.cuckoo.{CuckooFilterOps, CuckooStrategy}

import scala.util.Try


/** A mutable cuckoo filter. */
@SerialVersionUID(1L)
final class CuckooFilter[E] private(private var ops: CuckooFilterOps[E]) extends Filter[E] {
  def this(strategy: CuckooStrategy[E], seed: Int) = this(new CuckooFilterOps[E](true, t => CuckooTable.empty(t), strategy, seed))

  def this(strategy: CuckooStrategy[E]) = this(strategy, 0)

  // todo: make private
  def data: CuckooTable = ops.table.asInstanceOf[CuckooTable]

  def strategy: CuckooStrategy[E] = ops.strategy

  override def size(): Int = ops.table.typed.size

  override def capacity(): Int = ops.strategy.capacity

  override def fpp(): Double = ops.strategy.fpp

  override def contains(elem: E): Boolean = ops.contains(elem)

  override def add(elem: E): CuckooFilter[E] = {ops.add(elem); this}

  def add[T](triple: CuckooStrategy.Triple, entry: T, elem: Any): CuckooFilter[E] = {ops.add[T](triple, entry, elem); this}

  override def tryAdd(elem: E): Try[CuckooFilter[E]] = Try.apply(add(elem))

  override def remove(elem: E): CuckooFilter[E] = {ops.remove(elem); this}

  /**
   * @return `this` with hash table being the final result of `op`
   * @see [[probfilter.pdsa.cuckoo.mutable.TypedCuckooTable.zipFold]]
   * @note `op` is applied iteratively from left to right, so please be mindful of the order of other reads/writes.
   */
  def zipFold[T](that: CuckooFilter[E])
                (z: TypedCuckooTable[T])
                (op: (TypedCuckooTable[T], Array[T], Array[T], Int) => TypedCuckooTable[T]): CuckooFilter[E] = {
    val thisTable = this.ops.table.asInstanceOf[CuckooTable].typed[T]
    val thatTable = that.ops.table.asInstanceOf[CuckooTable].typed[T]
    val maxBuckets = math.max(thisTable.numBuckets, thatTable.numBuckets)
    val newTable = thisTable.zipFold(thatTable)(z.reserve(maxBuckets))(op)
    ops = this.ops.copy(newTable)
    this
  }

  def rebalance(repeat: Int = 3): CuckooFilter[E] = {
    ops = Range.apply(0, repeat).foldLeft(ops) { (ops, _) => ops.copy(ops.rebalance()) }
    this
  }

  override def toString: String = s"CF(${ops.table})"
}
