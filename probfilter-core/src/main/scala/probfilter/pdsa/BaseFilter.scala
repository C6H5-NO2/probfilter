package probfilter.pdsa


trait BaseFilter[T] extends Serializable {
  def add(elem: T): Boolean

  def mightContains(elem: T): Boolean

  def remove(elem: T): Boolean = throw new UnsupportedOperationException("BaseFilter.remove")
}
