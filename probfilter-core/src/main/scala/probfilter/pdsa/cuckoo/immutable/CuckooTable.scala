package probfilter.pdsa.cuckoo.immutable

import probfilter.pdsa.cuckoo.CuckooTableOps
import probfilter.util.JavaFriendly

import scala.reflect.ClassTag


/** An immutable cuckoo hash table for cuckoo filters. */
trait CuckooTable extends CuckooTableOps {
  override def typed[T]: TypedCuckooTable[T] = this.asInstanceOf[TypedCuckooTable[T]]
}


object CuckooTable {
  /** @see [[scala.reflect.ClassTag.Unit]] */
  def empty[T: ClassTag]: CuckooTable = TypedCuckooTable.empty[T]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.immutable.CuckooTable.empty")
  def emptyByte: CuckooTable = empty[Byte]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.immutable.CuckooTable.empty")
  def emptyShort: CuckooTable = empty[Short]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.immutable.CuckooTable.empty")
  def emptyInt: CuckooTable = empty[Int]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.immutable.CuckooTable.empty")
  def emptyLong: CuckooTable = empty[Long]
}
