package probfilter.pdsa.cuckoo

import probfilter.util.UnsignedNumber
import probfilter.util.UnsignedVal._


/**
 * An immutable wrapper of 64-bit unsigned long representing an entry in the cuckoo table.
 * This class is a value class which avoids runtime object allocation in Scala.
 */
final class VersionedEntry(private val data: Long) extends AnyVal {
  /** @return 16-bit unsigned fingerprint */
  @inline def fingerprint: Short = ((data >>> 48) & 0xffffL).toShort

  /** @return 16-bit unsigned replica id */
  @inline def replicaId: Short = ((data >>> 32) & 0xffffL).toShort

  /** @return 32-bit unsigned timestamp */
  @inline def timestamp: Int = (data & 0xffff_ffffL).toInt

  /** @return `true` if `this` is partially less than `that`; `false` otherwise */
  @inline def lt(that: VersionedEntry): Boolean =
    ((data >>> 32) == (that.toLong >>> 32)) && (UnsignedNumber.compare(data, that.toLong) < 0)

  @inline def toLong: Long = data

  override def toString: String =
    s"VE(f${fingerprint.toUString}, r${replicaId.toUString}, t${timestamp.toUString})"
}


object VersionedEntry {
  @inline def create(data: Long): VersionedEntry = new VersionedEntry(data)

  @inline def create(fingerprint: Short, replicaId: Short, timestamp: Int): VersionedEntry = {
    val data = parse(fingerprint, replicaId, timestamp)
    new VersionedEntry(data)
  }

  @inline def parse(fingerprint: Short, replicaId: Short, timestamp: Int): Long = {
    val fp = (fingerprint.toLong & 0xffffL) << 48
    val id = (replicaId.toLong & 0xffffL) << 32
    val ts = timestamp.toLong & 0xffff_ffffL
    fp | id | ts
  }

  /** Extracts the fingerprint from `data`. */
  @inline def extract(data: Long): Short = ((data >>> 48) & 0xffffL).toShort
}
