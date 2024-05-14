package probfilter.crdt.immutable

import probfilter.pdsa.cuckoo.CuckooStrategy
import probfilter.util.FalseRandom

import scala.annotation.tailrec


/** An immutable scalable observed-remove replicated cuckoo filter. */
@SerialVersionUID(1L)
final class ScORCuckooFilter[E] private
(state: Series[ORCuckooFilter[E]], val rid: Short, val strategy: CuckooStrategy[E])
  extends CvFilterSeries[E, ORCuckooFilter[E]](state) {
  /**
   * @param strategy a strategy whose fpp is the expected compounded value
   * @note The first sub-filter is created with `strategy.tighten()`. Note the initial `capacity` and `fingerprintBits`.
   */
  def this(strategy: CuckooStrategy[E], rid: Short) =
    this(Series.create(new ORCuckooFilter[E](strategy.tighten(), rid)), rid, strategy)

  override def fpp(): Double = strategy.fpp()

  @tailrec
  override def add(elem: E): ScORCuckooFilter[E] = {
    val expanded = expand().asInstanceOf[ScORCuckooFilter[E]].state
    val w = expanded.series.length
    val copied = if (w < 2) expanded else expanded.map(w - 1)(f => f.copy(expanded.get(w - 1 - 1).hist))
    var i = w - 1
    while (i >= 0) {
      val filter = copied.get(i)
      if (filter.size() < filter.capacity()) {
        val res = filter.tryAdd(elem)
        if (res.isSuccess) {
          val added = res.get
          val updated = copied.set(i, added)
          val updated2 = updated.map(f => f.copy(added.hist))
          return copy(updated2)
        }
      }
      i -= 1
    }
    expand(true).asInstanceOf[ScORCuckooFilter[E]].add(elem)
  }

  override def remove(elem: E): ScORCuckooFilter[E] = {
    val indexes = state.series.zipWithIndex.withFilter(_._1.contains(elem)).map(_._2)
    if (indexes.isEmpty) {
      this
    } else {
      val i = indexes.apply(FalseRandom.next(indexes.length))
      val updated = state.map(i)(f => f.remove(elem))
      copy(updated).shrink().asInstanceOf[ScORCuckooFilter[E]]
    }
  }

  override protected def nextSubFilter(): ORCuckooFilter[E] = {
    if (state.series.isEmpty)
      new ORCuckooFilter[E](strategy.tighten(), rid)
    else
      new ORCuckooFilter[E](state.series.last.strategy.tighten(), rid)
  }

  override protected def copy(state: Series[ORCuckooFilter[E]]): ScORCuckooFilter[E] = {
    new ScORCuckooFilter[E](state, rid, strategy)
  }
}
