package probfilter.crdt.immutable

import scala.util.Try


trait BaseFilter[E, T <: BaseFilter[E, T]] extends Serializable {
  def mightContains(elem: E): Boolean

  def add(elem: E): Try[T]

  def remove(elem: E): Try[T] = throw new UnsupportedOperationException("BaseFilter.remove")

  def merge(that: T): T
}
