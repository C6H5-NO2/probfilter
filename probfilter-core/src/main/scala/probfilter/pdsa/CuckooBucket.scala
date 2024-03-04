package probfilter.pdsa


/** An immutable view of a bucket in [[probfilter.pdsa.CuckooTable]]. */
trait CuckooBucket[@specialized(Byte, Short, Long) E] {
  /** @return index of this bucket */
  def at: Int

  /** @return iterator of entries in this bucket */
  def iterator: Iterator[E]

  /** @return number of entries in this bucket */
  def size: Int = iterator.size

  def contains(e: E): Boolean = iterator.contains(e)

  /**
   * @return a table with entry `e` added to this bucket
   * @throws java.lang.IllegalArgumentException if `e` is zero
   */
  def add(e: E): CuckooTable[E]

  /**
   * @return a table with entry `e` removed from this bucket
   * @throws java.lang.IllegalArgumentException if `e` is zero
   */
  def remove(e: E): CuckooTable[E]

  /**
   * Replaces the entry at index `victim` with `e`.
   *
   * @return a tuple of the replaced entry and the updated table
   * @throws java.lang.RuntimeException if `victim` is out of range
   */
  def replace(e: E, victim: Int): (E, CuckooTable[E])

  /**
   * Pops an entry from this bucket.
   *
   * @return a tuple of the popped entry and the updated table
   * @throws java.lang.RuntimeException if this bucket is empty
   */
  def pop(): (E, CuckooTable[E])
}
