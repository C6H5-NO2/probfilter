package probfilter.util


trait Mergeable[T] {
  def merge(that: T): T
}
