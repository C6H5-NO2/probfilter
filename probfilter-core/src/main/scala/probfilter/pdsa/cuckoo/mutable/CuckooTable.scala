package probfilter.pdsa.cuckoo.mutable

import probfilter.pdsa.cuckoo.CuckooTableOps
import probfilter.util.JavaFriendly

import scala.reflect.ClassTag


/** A mutable cuckoo hash table for cuckoo filters. */
trait CuckooTable extends CuckooTableOps {
  override def typed[T]: TypedCuckooTable[T] = this.asInstanceOf[TypedCuckooTable[T]]
}


object CuckooTable {
  /** @see [[scala.reflect.ClassTag.Unit]] */
  def empty[T: ClassTag]: CuckooTable = TypedCuckooTable.empty[T]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.mutable.CuckooTable.empty")
  def emptyByte: CuckooTable = empty[Byte]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.mutable.CuckooTable.empty")
  def emptyShort: CuckooTable = empty[Short]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.mutable.CuckooTable.empty")
  def emptyInt: CuckooTable = empty[Int]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.mutable.CuckooTable.empty")
  def emptyLong: CuckooTable = empty[Long]
}
