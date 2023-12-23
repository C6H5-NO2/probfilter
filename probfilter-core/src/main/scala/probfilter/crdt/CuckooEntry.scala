package probfilter.crdt

import probfilter.util.UnsignedString

import java.util.Optional


class CuckooEntry(val fingerprint: Byte, val replicaId: Short, val timestamp: Int)
  extends PartiallyOrdered[CuckooEntry] {
  override def tryCompareTo[B >: CuckooEntry : AsPartiallyOrdered](that: B): Option[Int] = that match {
    case that: CuckooEntry => {
      if (this.fingerprint == that.fingerprint && this.replicaId == that.replicaId) {
        Some(Integer.compareUnsigned(this.timestamp, that.timestamp))
      } else {
        None
      }
    }
    case _ => throw new IllegalArgumentException(s"CuckooEntry.tryCompareTo: Illegal type $that")
  }

  def tryCompareToAsJava(that: CuckooEntry): Optional[Integer] = {
    tryCompareTo(that) match {
      case Some(o) => Optional.of(o)
      case None => Optional.empty()
    }
  }

  def toLong: Long = {
    val fp = (fingerprint.toLong & 0xff) << 48
    val id = (replicaId.toLong & 0xffff) << 32
    val ts = timestamp.toLong & 0xffff_ffff
    fp | id | ts
  }

  override def toString: String =
    s"E(f${UnsignedString.from(fingerprint)},p$replicaId,t${UnsignedString.from(timestamp)})"
}


object CuckooEntry {
  def fromLong(long: Long): CuckooEntry = {
    val fp = (long >>> 48) & 0xff
    val id = (long >>> 32) & 0xffff
    val ts = long & 0xffff_ffff
    new CuckooEntry(fp.toByte, id.toShort, ts.toInt)
  }

  def fpOf(long: Long): Byte = ((long >>> 48) & 0xff).toByte

  def fpEq(long: Long, fp: Byte): Boolean = ((long >>> 48) & 0xff).toByte == fp
}
