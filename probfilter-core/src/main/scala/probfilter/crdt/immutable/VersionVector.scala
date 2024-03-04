package probfilter.crdt.immutable

import probfilter.crdt.Convergent
import probfilter.util.UnsignedNumber
import probfilter.util.UnsignedVal._

import scala.collection.immutable.TreeMap


/** An immutable version vector. */
@SerialVersionUID(1L)
final class VersionVector private(private val version: TreeMap[Short, Int]) extends Convergent[VersionVector] {
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

  override def lteq(that: VersionVector): Boolean = {
    this.version.forall { case (id, ts) => !(ts gtu that.version.getOrElse(id, 0)) }
  }

  override def merge(that: VersionVector): VersionVector = {
    val v2 = this.version.foldLeft(that.version) { case (v2, (id, ts)) =>
      if (ts gtu v2.getOrElse(id, 0))
        v2.updated(id, ts)
      else
        v2
    }
    new VersionVector(v2)
  }

  override def toString: String =
    version.view.map { case (r, t) => s"${r.toUnsignedString}->${t.toUnsignedString}" }.mkString("V(", ", ", ")")
}
