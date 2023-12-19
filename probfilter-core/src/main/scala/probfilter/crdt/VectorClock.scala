package probfilter.crdt

import scala.collection.immutable.TreeMap


@SerialVersionUID(1L)
class VectorClock private(private val clock: TreeMap[Short, Int]) extends Serializable {
  def this() = this(TreeMap.empty)

  def get(replicaId: Short): Int = clock.getOrElse(replicaId, 0)

  def inc(replicaId: Short): VectorClock = {
    val ts = clock.getOrElse(replicaId, 0) + 1
    new VectorClock(clock.updated(replicaId, ts))
  }

  def merge(that: VectorClock): VectorClock = {
    var clock2 = that.clock
    for ((id, ts) <- this.clock) {
      if (Integer.compareUnsigned(ts, clock2.getOrElse(id, 0)) > 0)
        clock2 = clock2.updated(id, ts)
    }
    new VectorClock(clock2)
  }

  override def toString: String = ???
}
