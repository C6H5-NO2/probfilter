package probfilter.crdt.immutable

import probfilter.crdt.Convergent


/** An immutable CvRDT series. */
@SerialVersionUID(1L)
final class Series[T <: Convergent[T]] private(val series: Vector[T]) extends Convergent[Series[T]] {
  def this() = this(Vector.empty[T])

  override def lteq(that: Series[T]): Boolean = {
    this.series.size <= that.series.size && this.series.lazyZip(that.series).forall((x, y) => x.lteq(y))
  }

  override def merge(that: Series[T]): Series[T] = {
    val head = this.series.lazyZip(that.series).map((x, y) => x.merge(y))
    val tail = if (this.series.size > that.series.size) this.series.drop(that.series.size) else that.series.drop(this.series.size)
    new Series[T](head.concat(tail))
  }

  @inline def get(index: Int): T = series.apply(index)

  @inline def add(elem: T): Series[T] = new Series[T](series.appended(elem))

  @inline def removeLast(): Series[T] = new Series[T](series.dropRight(1))

  @inline def set(index: Int, elem: T): Series[T] = new Series[T](series.updated(index, elem))

  @inline def map(index: Int)(op: T => T): Series[T] = new Series[T](series.updated(index, op(series.apply(index))))

  @inline def map(op: T => T): Series[T] = new Series[T](series.map(op))

  override def toString: String = s"S($series)"
}


object Series {
  def empty[T <: Convergent[T]]: Series[T] = new Series[T](Vector.empty[T])

  def create[T <: Convergent[T]](head: T): Series[T] = new Series[T](Vector.apply[T](head))
}
