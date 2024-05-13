package probfilter.crdt.immutable

import scala.annotation.tailrec


/**
 * @tparam E type of elements
 * @tparam F type of sub-filters
 */
abstract class CvFilterSeries[E, F <: ImmCvFilter[E, F]] protected
(protected val state: Series[F]) extends ImmCvFilter[E, CvFilterSeries[E, F]] {
  override def size(): Int = state.series.foldLeft(0) { (sum, f) => sum + f.size() }

  override def capacity(): Int = Int.MaxValue

  override def contains(elem: E): Boolean = state.series.exists(f => f.contains(elem))

  override def lteq(that: CvFilterSeries[E, F]): Boolean = this.state.lteq(that.state)

  override def merge(that: CvFilterSeries[E, F]): CvFilterSeries[E, F] = copy(this.state.merge(that.state))

  override def toString: String = s"SFS($state)"

  /** @return the next sub-filter to be added to `state` */
  protected def nextSubFilter(): F

  /** @return a new instance constructed with `state` */
  protected def copy(state: Series[F]): CvFilterSeries[E, F]

  protected final def expand(): CvFilterSeries[E, F] = expand(false)

  protected final def expand(force: Boolean): CvFilterSeries[E, F] = {
    if (state.series.isEmpty) {
      val f = nextSubFilter()
      copy(state.add(f))
    } else {
      val last = state.series.last
      if (force || last.size() >= last.capacity()) {
        val f = nextSubFilter()
        copy(state.add(f))
      } else {
        this
      }
    }
  }

  @tailrec
  protected final def shrink(): CvFilterSeries[E, F] = {
    if (state.series.length <= 1)
      this
    else if (state.series.last.size() > 0)
      this
    else
      copy(state.removeLast()).shrink()
  }
}
