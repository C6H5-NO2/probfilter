package probfilter.crdt

import probfilter.util.UnsignedVal._
import probfilter.util.{Mergeable, UnsignedNumber}

import scala.collection.immutable.TreeMap


/**
 * An immutable vector clock.
 */
@SerialVersionUID(1L)
class VectorClock private(private val clock: TreeMap[Short, Int]) extends Mergeable[VectorClock] with Serializable {
  def this() = this(new TreeMap[Short, Int]()((x, y) => UnsignedNumber.compare(x, y)))

  /**
   * @param replicaId 16-bit unsigned id
   * @return 32-bit unsigned timestamp
   */
  def get(replicaId: Short): Int = clock.getOrElse(replicaId, 0)

  /**
   * @param replicaId 16-bit unsigned id
   */
  def inc(replicaId: Short): VectorClock = {
    val ts = clock.getOrElse(replicaId, 0) + 1
    new VectorClock(clock.updated(replicaId, ts))
  }

  override def merge(that: VectorClock): VectorClock = {
    var clock2 = that.clock
    for ((id, ts) <- this.clock) {
      if (ts gtu clock2.getOrElse(id, 0))
        clock2 = clock2.updated(id, ts)
    }
    new VectorClock(clock2)
  }

  override def toString: String =
    clock.view.map { case (r, t) => s"${r.toUnsignedString}->${t.toUnsignedString}" }.mkString("C(", ", ", ")")
}
