package probfilter.crdt.immutable

import probfilter.pdsa.cuckoo.{CuckooFilter, CuckooStrategy}


/** An immutable grow-only replicated cuckoo filter. */
@SerialVersionUID(1L)
final class GCuckooFilter[E] private(val state: CuckooFilter[E]) extends CvFilter[E, GCuckooFilter[E]] {
  def this(strategy: CuckooStrategy[E]) = this(new CuckooFilter[E](strategy))

  override def size(): Int = state.size()

  override def capacity(): Int = state.capacity()

  override def fpp(): Double = state.fpp()

  override def contains(elem: E): Boolean = state.contains(elem)

  override def add(elem: E): GCuckooFilter[E] = if (contains(elem)) this else copy(state.add(elem))

  override def lteq(that: GCuckooFilter[E]): Boolean = ???

  override def merge(that: GCuckooFilter[E]): GCuckooFilter[E] = {
    val thisData = this.state.data.typed
    val thatData = that.state.data.typed
    val newData = thisData.zipFold(thatData)(thisData) { (newData, thisBucket, thatBucket, index) =>
      thatBucket.foldLeft(newData) { (newData, entry) =>
        if (thisBucket.contains(entry)) {
          newData
        } else {
          val altIndex = this.state.strategy.altIndexOf(index, entry)
          if (thisData.contains(altIndex, entry))
            newData
          else
            newData.add(altIndex, entry)
        }
      }
    }
    copy(state.copy(newData))
  }

  def copy(state: CuckooFilter[E]): GCuckooFilter[E] = new GCuckooFilter[E](state)

  override def toString: String = s"GCF($state)"
}
