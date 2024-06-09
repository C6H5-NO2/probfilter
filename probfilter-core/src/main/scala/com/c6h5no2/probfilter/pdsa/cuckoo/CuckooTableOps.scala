package probfilter.pdsa.cuckoo


/** @see [[probfilter.pdsa.cuckoo.TypedCuckooTableOps]] */
trait CuckooTableOps extends Serializable {
  def typed[T]: TypedCuckooTableOps[T] = this.asInstanceOf[TypedCuckooTableOps[T]]
}
