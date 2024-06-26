package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.util
import com.c6h5no2.probfilter.util.ClassEx.Clazz
import com.c6h5no2.probfilter.util.UnsignedNumber

import scala.collection.immutable.TreeMap


/** An immutable version vector. */
@SerialVersionUID(1L)
final class VersionVector private(
  private val version: TreeMap[Short, Int]
) extends CausalHistory
  with CvRDT[VersionVector]
  with util.Immutable {
  def this() = this(new TreeMap[Short, Int]()((x, y) => UnsignedNumber.compare(x, y)))

  override def get(replicaId: Short): Int = version.getOrElse(replicaId, 0)

  override def increase(replicaId: Short): VersionVector =
    new VersionVector(version.updated(replicaId, next(replicaId)))

  override def lteq(that: VersionVector): Boolean =
    this.version.forall(tup => that.observes(tup._1, tup._2))

  override def merge(that: VersionVector): VersionVector = {
    val v2 = this.version.foldLeft(that.version) { (v2, tup) =>
      val id = tup._1
      val ts = tup._2
      if (UnsignedNumber.compare(ts, v2.getOrElse(id, 0)) > 0)
        v2.updated(id, ts)
      else
        v2
    }
    new VersionVector(v2)
  }

  override def toString: String = {
    version
      .view
      .map(tup => s"${UnsignedNumber.toString(tup._1)}->${UnsignedNumber.toString(tup._2)}")
      .mkString(s"${getClass.getShortName}(", ", ", ")")
  }
}

object VersionVector {
  def apply(): VersionVector = new VersionVector()
}
