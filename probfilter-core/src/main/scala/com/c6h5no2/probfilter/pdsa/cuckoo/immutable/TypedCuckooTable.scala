package probfilter.pdsa.cuckoo.immutable

import probfilter.pdsa.cuckoo.TypedCuckooTableOps

import scala.reflect.ClassTag


trait TypedCuckooTable[T] extends TypedCuckooTableOps[T] with CuckooTable {
  override def set(index: Int, value: Array[T]): TypedCuckooTable[T]

  override def reserve(buckets: Int): TypedCuckooTable[T] = this // no-op for immutable cuckoo tables

  override def add(index: Int, entry: T): TypedCuckooTable[T] =
    super.add(index, entry).asInstanceOf[TypedCuckooTable[T]]

  override def remove(index: Int, entry: T): TypedCuckooTable[T] =
    super.remove(index, entry).asInstanceOf[TypedCuckooTable[T]]

  override def replace(index: Int, entry: T, victimIndex: Int): (T, TypedCuckooTable[T]) =
    super.replace(index, entry, victimIndex).asInstanceOf[(T, TypedCuckooTable[T])]
}


object TypedCuckooTable {
  def empty[T: ClassTag]: TypedCuckooTable[T] = MapCuckooTable.empty[T]
}
