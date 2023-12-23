package probfilter.crdt.immutable

import probfilter.util.Mergeable

import scala.util.Try


trait BaseFilter[E, T <: BaseFilter[E, T] with Mergeable[T]] extends Serializable {
  def mightContains(elem: E): Boolean

  def add(elem: E): Try[T]

  def remove(elem: E): T = throw new UnsupportedOperationException("BaseFilter.remove")
}
