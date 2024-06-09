package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.util.UnsignedNumber


/**
 * An immutable 64-bit cuckoo entry comprises 16-bit fingerprint, 16-bit replica id, and 32-bit timestamp.
 *
 * @note This class is a value class which avoids runtime object allocation (in some cases).
 */
final class LongVersionedEntry(private val data: Long) extends AnyVal {
  /** @return 16-bit unsigned fingerprint */
  @inline def fingerprint: Short = ((data >>> 48) & 0xffffL).toShort

  /** @return 16-bit unsigned replica id */
  @inline def replicaId: Short = ((data >>> 32) & 0xffffL).toShort

  /** @return 32-bit unsigned timestamp */
  @inline def timestamp: Int = (data & 0xffff_ffffL).toInt

  /** @return `true` if `this` is partially less than `that`; `false` otherwise */
  @inline def lt(that: LongVersionedEntry): Boolean =
    ((data >>> 32) == (that.toLong >>> 32)) && (UnsignedNumber.compare(data, that.toLong) < 0)

  @inline def toLong: Long = data

  override def toString: String = {
    val f = UnsignedNumber.toString(fingerprint)
    val r = UnsignedNumber.toString(replicaId)
    val t = UnsignedNumber.toString(timestamp)
    s"(f$f, r$r, t$t)"
  }
}

object LongVersionedEntry {
  @inline def apply(data: Long): LongVersionedEntry = new LongVersionedEntry(data)

  @inline def apply(fingerprint: Short, replicaId: Short, timestamp: Int): LongVersionedEntry = {
    val data = parse(fingerprint, replicaId, timestamp)
    new LongVersionedEntry(data)
  }

  @inline def parse(fingerprint: Short, replicaId: Short, timestamp: Int): Long = {
    val fp = (fingerprint.toLong & 0xffffL) << 48
    val id = (replicaId.toLong & 0xffffL) << 32
    val ts = timestamp.toLong & 0xffff_ffffL
    fp | id | ts
  }

  @inline def extract(data: Long): Short = ((data >>> 48) & 0xffffL).toShort
}
