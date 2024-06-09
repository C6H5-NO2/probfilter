package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.util.UnsignedNumber


/**
 * An immutable 32-bit cuckoo entry comprises 8-bit fingerprint, 4-bit replica id, and 20-bit timestamp.
 *
 * @note This class is a value class which avoids runtime object allocation (in some cases).
 */
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

  override def toString: String = {
    val f = UnsignedNumber.toString(fingerprint)
    val r = UnsignedNumber.toString(replicaId)
    val t = UnsignedNumber.toString(timestamp)
    s"(f$f, r$r, t$t)"
  }
}

object IntVersionedEntry {
  @inline def apply(data: Int): IntVersionedEntry = new IntVersionedEntry(data)

  @inline def apply(fingerprint: Short, replicaId: Short, timestamp: Int): IntVersionedEntry = {
    val data = parse(fingerprint, replicaId, timestamp)
    new IntVersionedEntry(data)
  }

  @inline def parse(fingerprint: Short, replicaId: Short, timestamp: Int): Int = {
    val fp = (fingerprint.toInt & 0xff) << 24
    val id = (replicaId.toInt & 0xf) << 20
    val ts = timestamp & 0x000f_ffff
    fp | id | ts
  }

  @inline def extract(data: Int): Short = ((data >>> 24) & 0xff).toShort
}
