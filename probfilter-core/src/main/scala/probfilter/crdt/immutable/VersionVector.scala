package probfilter.crdt.immutable

import probfilter.crdt.Mergeable
import probfilter.util.UnsignedNumber
import probfilter.util.UnsignedVal._

import scala.collection.immutable.TreeMap


/**
 * An immutable version vector.
 */
@SerialVersionUID(1L)
final class VersionVector private(private val version: TreeMap[Short, Int])
  extends Mergeable[VersionVector] with Serializable {
  def this() = this(new TreeMap[Short, Int]()((x, y) => UnsignedNumber.compare(x, y)))

  /**
   * @param replicaId 16-bit unsigned id
   * @return 32-bit unsigned timestamp
   */
  def get(replicaId: Short): Int = version.getOrElse(replicaId, 0)

  /**
   * @param replicaId 16-bit unsigned id
   */
  def inc(replicaId: Short): VersionVector = {
    val ts = version.getOrElse(replicaId, 0) + 1
    new VersionVector(version.updated(replicaId, ts))
  }

  override def merge(that: VersionVector): VersionVector = {
    var clock2 = that.version
    for ((id, ts) <- this.version) {
      if (ts gtu clock2.getOrElse(id, 0))
        clock2 = clock2.updated(id, ts)
    }
    new VersionVector(clock2)
  }

  override def toString: String =
    version.view.map { case (r, t) => s"${r.toUnsignedString}->${t.toUnsignedString}" }.mkString("V(", ", ", ")")
}
