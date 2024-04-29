package probfilter.pdsa.cuckoo

import probfilter.util.UnsignedNumber
import probfilter.util.UnsignedVal._


@Deprecated
final class IntVersionedEntry(private val data: Int) extends AnyVal {
  /** @return 8-bit unsigned fingerprint */
  @inline def fingerprint: Short = ((data >>> 24) & 0xff).toShort

  /** @return 4-bit unsigned replica id */
  @inline def replicaId: Short = ((data >>> 20) & 0xf).toShort

  /** @return 20-bit unsigned timestamp */
  @inline def timestamp: Int = data & 0x000f_ffff

  /** @return `true` if `this` is partially less than `that`; `false` otherwise */
  @inline def lt(that: IntVersionedEntry): Boolean =
    ((data >>> 20) == (that.toInt >>> 20)) && (UnsignedNumber.compare(data, that.toInt) < 0)

  @inline def toInt: Int = data

  override def toString: String =
    s"VE(f${fingerprint.toUString}, r${replicaId.toUString}, t${timestamp.toUString})"
}


object IntVersionedEntry {
  @inline def create(data: Int): IntVersionedEntry = new IntVersionedEntry(data)

  @inline def create(fingerprint: Short, replicaId: Short, timestamp: Int): IntVersionedEntry = {
    val data = parse(fingerprint, replicaId, timestamp)
    new IntVersionedEntry(data)
  }

  @inline def parse(fingerprint: Short, replicaId: Short, timestamp: Int): Int = {
    val fp = (fingerprint.toInt & 0xff) << 24
    val id = (replicaId.toInt & 0xf) << 20
    val ts = timestamp & 0x000f_ffff
    fp | id | ts
  }

  /** Extracts the fingerprint from `data`. */
  @inline def extract(data: Int): Short = ((data >>> 24) & 0xff).toShort
}
