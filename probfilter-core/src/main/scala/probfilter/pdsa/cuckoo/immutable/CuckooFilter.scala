package probfilter.pdsa.cuckoo.immutable

import probfilter.pdsa.Filter
import probfilter.pdsa.cuckoo.{CuckooFilterOps, CuckooStrategy, CuckooTableOps, EntryStorageType}

import scala.util.Try


/** An immutable cuckoo filter. */
@SerialVersionUID(1L)
final class CuckooFilter[E] private(private val ops: CuckooFilterOps[E]) extends Filter[E] {
  def this(strategy: CuckooStrategy[E]) = this(new CuckooFilterOps[E](
    strategy.storageType() match {
      case EntryStorageType.SIMPLE_BYTE => CuckooTable.empty[Byte]
      case EntryStorageType.SIMPLE_SHORT => CuckooTable.empty[Short]
      case EntryStorageType.VERSIONED_INT => CuckooTable.empty[Int]
      case EntryStorageType.VERSIONED_LONG => CuckooTable.empty[Long]
    },
    strategy,
    false
  ))

  // todo: make private
  def data: CuckooTable = ops.table.asInstanceOf[CuckooTable]

  def strategy: CuckooStrategy[E] = ops.strategy

  override def size(): Int = ops.table.typed.size

  override def capacity(): Int = ops.strategy.capacity

  override def fpp(): Double = ops.strategy.fpp

  override def contains(elem: E): Boolean = ops.contains(elem)

  override def add(elem: E): CuckooFilter[E] = copy(ops.add(elem))

  def add[T](triple: CuckooStrategy.Triple, entry: T, elem: Any): CuckooFilter[E] = copy(ops.add[T](triple, entry, elem))

  override def tryAdd(elem: E): Try[CuckooFilter[E]] = Try.apply(add(elem))

  override def remove(elem: E): CuckooFilter[E] = copy(ops.remove(elem))

  /**
   * @return a new instance of `CuckooFilter` with hash table being the final result of `op`
   * @see [[probfilter.pdsa.cuckoo.immutable.TypedCuckooTable.zipFold]]
   */
  def zipFold[T](that: CuckooFilter[E])
                (z: TypedCuckooTable[T])
                (op: (TypedCuckooTable[T], Array[T], Array[T], Int) => TypedCuckooTable[T]): CuckooFilter[E] = {
    val thisTable = this.ops.table.asInstanceOf[CuckooTable].typed[T]
    val thatTable = that.ops.table.asInstanceOf[CuckooTable].typed[T]
    val newTable = thisTable.zipFold(thatTable)(z)(op)
    copy(newTable)
  }

  private def copy(table: CuckooTableOps): CuckooFilter[E] =
    new CuckooFilter[E](new CuckooFilterOps[E](table, ops.strategy, false))

  override def toString: String = s"CF(${ops.table})"
}
