package probfilter.crdt.immutable

import probfilter.pdsa.cuckoo.CuckooStrategy

import scala.annotation.tailrec


/** An immutable scalable grow-only replicated cuckoo filter. */
@SerialVersionUID(1L)
final class ScGCuckooFilter[E] private
(state: Series[GCuckooFilter[E]], val strategy: CuckooStrategy[E])
  extends CvFilterSeries[E, GCuckooFilter[E]](state) {
  /**
   * @param strategy a strategy whose fpp is the expected compounded value
   * @note The first sub-filter is created with `strategy.tighten()`. Note the initial `capacity` and `fingerprintBits`.
   */
  def this(strategy: CuckooStrategy[E]) = this(Series.empty[GCuckooFilter[E]], strategy)

  override def fpp(): Double = strategy.fpp()

  @tailrec
  override def add(elem: E): ScGCuckooFilter[E] = {
    if (contains(elem)) {
      this
    } else {
      val expanded = expand().asInstanceOf[ScGCuckooFilter[E]].state
      val w = expanded.series.length
      var i = w - 1
      while (i >= 0) {
        val filter = expanded.get(i)
        if (filter.size() < filter.capacity()) {
          val res = filter.tryAdd(elem)
          if (res.isSuccess) {
            val added = res.get
            val updated = expanded.set(i, added)
            return copy(updated)
          }
        }
        i -= 1
      }
      expand(true).asInstanceOf[ScGCuckooFilter[E]].add(elem)
    }
  }

  override protected def nextSubFilter(): GCuckooFilter[E] = {
    if (state.series.isEmpty)
      new GCuckooFilter[E](strategy.tighten())
    else
      new GCuckooFilter[E](state.series.last.strategy.tighten())
  }

  override protected def copy(state: Series[GCuckooFilter[E]]): ScGCuckooFilter[E] = new ScGCuckooFilter[E](state, strategy)
}
