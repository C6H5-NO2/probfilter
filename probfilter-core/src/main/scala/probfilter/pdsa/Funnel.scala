package probfilter.pdsa


trait Funnel[T] extends Serializable {
  def funnel(from: T, into: Sink): Unit
}
