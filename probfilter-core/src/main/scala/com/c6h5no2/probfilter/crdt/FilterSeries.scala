package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.util.ClassEx.Clazz
import com.c6h5no2.probfilter.util.Immutable

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag


/**
 * An immutable series of [[CvRFilter]]s.
 *
 * @param supplier supplies the next sub-filter given the last sub-filter
 * @tparam E type of elements
 * @tparam F type of sub-filters
 */
@SerialVersionUID(1L)
final class FilterSeries[E, F <: CvRFilter[E, F] : ClassTag] private(
  private val series: ArraySeq[F],
  supplier: Option[F] => F,
) extends CvRDT[FilterSeries[E, F]] with Immutable {
  def this(supplier: Option[F] => F) = this(ArraySeq.empty[F], supplier)

  def size: Int = series.foldLeft(0) { (sum, sf) => sum + sf.size }

  def capacity: Int = Int.MaxValue

  def contains(elem: E): Boolean = series.exists(sf => sf.contains(elem))

  def expand(): FilterSeries[E, F] = expand(false)

  def expand(force: Boolean): FilterSeries[E, F] = {
    if (series.isEmpty) {
      val sf = supplier.apply(None)
      copy(series.appended(sf))
    } else {
      val last = series.last
      if (force || last.size >= last.capacity) {
        val sf = supplier.apply(Some(last))
        copy(series.appended(sf))
      } else {
        this
      }
    }
  }

  @tailrec
  def shrink(): FilterSeries[E, F] = {
    if (series.length <= 1)
      this
    else if (series.last.size > 0)
      this
    else
      copy(series.dropRight(1)).shrink()
  }

  /** @return number of sub-filters */
  @inline def length: Int = series.length

  @inline def reverseIterator: Iterator[F] = series.reverseIterator

  /** @throws java.lang.IndexOutOfBoundsException . */
  @inline def head: F = series.apply(0)

  /** @throws java.lang.IndexOutOfBoundsException . */
  @inline def last: F = series.apply(series.length - 1)

  /** @throws java.lang.IndexOutOfBoundsException . */
  @inline def set(index: Int, filter: F): FilterSeries[E, F] = copy(series.updated(index, filter))

  /** @throws java.lang.IndexOutOfBoundsException . */
  @inline def setLast(filter: F): FilterSeries[E, F] = set(series.length - 1, filter)

  /** @throws java.lang.IndexOutOfBoundsException . */
  @inline def update(index: Int, op: F => F): FilterSeries[E, F] =
    copy(series.updated(index, op.apply(series.apply(index))))

  /** @throws java.lang.IndexOutOfBoundsException . */
  @inline def updateLast(op: F => F): FilterSeries[E, F] = update(series.length - 1, op)

  /** Applies `op` to each sub-filter in this series and collects the results. */
  @inline def map(op: F => F): FilterSeries[E, F] = copy(series.map(op))

  override def lteq(that: FilterSeries[E, F]): Boolean = {
    this.series.length <= that.series.length && this.series.lazyZip(that.series).forall(_ lteq _)
  }

  override def merge(that: FilterSeries[E, F]): FilterSeries[E, F] = {
    val diff = that.series.length - this.series.length
    if (diff < 0) {
      val thatPadded = Range.apply(0, -diff).foldLeft(that)((s, _) => s.expand(true))
      val newSeries = this.series.lazyZip(thatPadded.series).map(_ merge _)
      copy(newSeries)
    } else {
      val thisPadded = Range.apply(0, diff).foldLeft(this)((s, _) => s.expand(true))
      val newSeries = thisPadded.series.lazyZip(that.series).map(_ merge _)
      copy(newSeries)
    }
  }

  /**
   * Different from [[FilterSeries.merge]] that merges exceeded sub-filters with empty ones (bottom),
   * this function directly shares those sub-filters. Such behaviour may or may not be correct
   * depending on the properties of the specific sub-filter.
   */
  @tailrec
  def fastmerge(that: FilterSeries[E, F]): FilterSeries[E, F] = {
    val diff = that.series.length - this.series.length
    if (diff < 0) {
      that.fastmerge(this)
    } else {
      val head = this.series.lazyZip(that.series).map(_ merge _)
      val tail = that.series.drop(this.series.length)
      copy(head.concat(tail))
    }
  }

  def copy(supplier: Option[F] => F): FilterSeries[E, F] = {
    new FilterSeries[E, F](this.series, supplier)
  }

  private def copy(series: ArraySeq[F]): FilterSeries[E, F] = {
    new FilterSeries[E, F](series, this.supplier)
  }

  override def toString: String = s"${getClass.getShortName}($series)"
}
