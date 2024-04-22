package probfilter.crdt.immutable

import probfilter.pdsa.bloom.BloomStrategy


/** An immutable scalable grow-only replicated bloom filter. */
@SerialVersionUID(1L)
final class ScGBloomFilter[E] private
(state: Series[GBloomFilter[E]], val strategy: BloomStrategy[E]) extends CvFilterSeries[E, GBloomFilter[E]](state) {
  /** @param strategy a strategy whose fpp is the expected compounded value */
  def this(strategy: BloomStrategy[E]) = this(Series.empty[GBloomFilter[E]], strategy)

  override def fpp(): Double = strategy.fpp()

  override def add(elem: E): ScGBloomFilter[E] = {
    if (contains(elem)) {
      this
    } else {
      val expanded = expand().asInstanceOf[ScGBloomFilter[E]].state
      val w = expanded.series.length
      copy(expanded.map(w - 1)(f => f.add(elem)))
    }
  }

  override protected def nextSubFilter(): GBloomFilter[E] = {
    if (state.series.isEmpty)
      new GBloomFilter[E](strategy.tighten())
    else
      new GBloomFilter[E](state.series.last.state.strategy.tighten())
  }

  override protected def copy(state: Series[GBloomFilter[E]]): ScGBloomFilter[E] = new ScGBloomFilter[E](state, strategy)
}
