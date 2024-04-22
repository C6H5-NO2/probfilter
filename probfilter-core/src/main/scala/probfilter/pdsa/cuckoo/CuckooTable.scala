package probfilter.pdsa.cuckoo

import probfilter.util.JavaFriendly

import scala.reflect.ClassTag


/**
 * An immutable cuckoo hash table for cuckoo filters.
 *
 * @see [[probfilter.pdsa.cuckoo.TypedCuckooTable]]
 */
trait CuckooTable extends Serializable {
  def typed[T]: TypedCuckooTable[T] = this.asInstanceOf[TypedCuckooTable[T]]
}


object CuckooTable {
  def empty[T: ClassTag]: CuckooTable = TypedCuckooTable.empty[T]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.CuckooTable.empty")
  def emptyByte: CuckooTable = empty[Byte]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.CuckooTable.empty")
  def emptyShort: CuckooTable = empty[Short]

  @JavaFriendly(scalaDelegate = "probfilter.pdsa.cuckoo.CuckooTable.empty")
  def emptyLong: CuckooTable = empty[Long]
}
