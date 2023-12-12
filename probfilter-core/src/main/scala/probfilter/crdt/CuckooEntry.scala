package probfilter.crdt

import java.util.Optional


case class CuckooEntry(fingerprint: Byte, timestamp: Long, replicaId: Long) extends PartiallyOrdered[CuckooEntry] with Serializable {
  override def tryCompareTo[B >: CuckooEntry : AsPartiallyOrdered](that: B): Option[Int] = that match {
    case that: CuckooEntry => {
      if (this.fingerprint == that.fingerprint && this.replicaId == that.replicaId) {
        Some(java.lang.Long.compare(this.timestamp, that.timestamp))
      } else {
        None
      }
    }
    case _ => throw new IllegalArgumentException(s"CuckooEntry.tryCompareTo: Invalid parameter type for $that")
  }

  def tryCompareWith(that: CuckooEntry): Optional[Integer] = {
    tryCompareTo(that) match {
      case Some(o) => Optional.of(o)
      case None => Optional.empty()
    }
  }
}
