package probfilter.crdt.immutable

import probfilter.crdt.BaseFilter
import probfilter.pdsa.{CuckooFilterOps, CuckooStrategy, CuckooTable}


/** An immutable grow-only replicated cuckoo filter. */
@SerialVersionUID(1L)
final class GCuckooFilter[E] private
// todo: Short -> Byte | Short
(val strategy: CuckooStrategy[E], val data: CuckooTable[Short]) extends BaseFilter[E, GCuckooFilter[E]] {
  def this(strategy: CuckooStrategy[E]) = this(strategy, CuckooTable.empty[Short])

  override def size(): Int = data.size

  private def containsTriple(triple: CuckooStrategy.Triple): Boolean = {
    data.at(triple.i).contains(triple.fp) || data.at(triple.j).contains(triple.fp)
  }

  override def contains(elem: E): Boolean = {
    val triple = strategy.getCuckooTriple(elem)
    containsTriple(triple)
  }

  override def add(elem: E): GCuckooFilter[E] = {
    val triple = strategy.getCuckooTriple(elem)
    if (containsTriple(triple))
      return this
    val newData = CuckooFilterOps.add(triple, triple.fp, data)(strategy, elem)
    new GCuckooFilter(strategy, newData)
  }

  override def lteq(that: GCuckooFilter[E]): Boolean = ???

  override def merge(that: GCuckooFilter[E]): GCuckooFilter[E] = {
    val newData = (0 until strategy.numBuckets).foldLeft(this.data) { (newData, i) =>
      val thisBucket = this.data.at(i)
      val thatBucket = that.data.at(i)
      thatBucket.iterator.foldLeft(newData) { (s, e) =>
        if (thisBucket.contains(e)) {
          s
        } else {
          // todo: entry -> fp -> entry
          val alt = strategy.getAltBucket(e, i)
          if (this.data.at(alt).contains(e))
            s
          else
            s.at(i).add(e)
        }
      }
    }

    new GCuckooFilter[E](this.strategy, newData)
  }
}
