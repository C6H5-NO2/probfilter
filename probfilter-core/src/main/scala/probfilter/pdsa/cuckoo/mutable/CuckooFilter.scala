package probfilter.pdsa.cuckoo.mutable

import probfilter.pdsa.Filter
import probfilter.pdsa.cuckoo.{CuckooFilterOps, CuckooStrategy, EntryStorageType}

import scala.util.Try


/** A mutable cuckoo filter. */
@SerialVersionUID(1L)
final class CuckooFilter[E] private(private var ops: CuckooFilterOps[E]) extends Filter[E] {
  def this(strategy: CuckooStrategy[E]) = this(new CuckooFilterOps[E](
    strategy.storageType() match {
      case EntryStorageType.SIMPLE_BYTE => CuckooTable.empty[Byte]
      case EntryStorageType.SIMPLE_SHORT => CuckooTable.empty[Short]
      case EntryStorageType.VERSIONED_INT => CuckooTable.empty[Int]
      case EntryStorageType.VERSIONED_LONG => CuckooTable.empty[Long]
    },
    strategy,
    true
  ))

  // todo: make private
  def data: CuckooTable = ops.table.asInstanceOf[CuckooTable]

  def strategy: CuckooStrategy[E] = ops.strategy

  override def size(): Int = ops.table.typed.size

  override def capacity(): Int = ops.strategy.capacity

  override def fpp(): Double = ops.strategy.fpp

  override def contains(elem: E): Boolean = ops.contains(elem)

  def exists[T](triple: CuckooStrategy.Triple, p: T => Boolean): Boolean = ops.exists(triple, p)

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
    val newTable = thisTable.zipFold(thatTable)(z)(op)
    ops = new CuckooFilterOps[E](newTable, ops.strategy, true)
    this
  }

  override def toString: String = s"CF(${ops.table})"
}
