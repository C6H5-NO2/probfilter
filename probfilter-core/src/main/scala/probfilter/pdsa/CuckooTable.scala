package probfilter.pdsa

import scala.reflect.ClassTag


/**
 * An immutable table for cuckoo filters.
 *
 * @see [[probfilter.pdsa.MapCuckooTable]], [[probfilter.pdsa.ArrayCuckooTable]]
 */
trait CuckooTable[@specialized(Byte, Short, Long) E] extends Serializable {
  def at(i: Int): CuckooBucket[E]

  def size: Int
}


object CuckooTable {
  def empty[@specialized(Byte, Short, Long) E: ClassTag]: CuckooTable[E] = new MapCuckooTable[E]()
}
