package probfilter.pdsa

import probfilter.util.UnsignedNumber
import probfilter.util.UnsignedVal._


/**
 * An immutable wrapper of 64-bit unsigned long representing an entry in the cuckoo table.
 * This class is also a value class which avoids runtime object allocation.
 */
final class LongCuckooEntry(private val data: Long) extends AnyVal {
  /** @return (less-than-)16-bit unsigned fingerprint */
  @inline def fingerprint: Short = ((data >>> 48) & 0xffffL).toShort

  /** @return 16-bit unsigned replica id */
  @inline def replicaId: Short = ((data >>> 32) & 0xffffL).toShort

  /** @return 32-bit unsigned timestamp */
  @inline def timestamp: Int = (data & 0xffff_ffffL).toInt

  @deprecated("Use `lt` or `gt`.")
  @inline
  def tryCompareTo(that: LongCuckooEntry): Option[Int] = LongCuckooEntry.Ordering.tryCompare(data, that.toLong)

  /** @return `true` if `this` is partially less than `that`; `false` otherwise */
  @inline def lt(that: LongCuckooEntry): Boolean =
    ((data >>> 32) == (that.toLong >>> 32)) && (UnsignedNumber.compare(data, that.toLong) < 0)

  /** @return `true` if `this` is partially greater than `that`; `false` otherwise */
  @inline def gt(that: LongCuckooEntry): Boolean =
    ((data >>> 32) == (that.toLong >>> 32)) && (UnsignedNumber.compare(data, that.toLong) > 0)

  @inline def toLong: Long = data

  override def toString: String =
    s"E(f${fingerprint.toUnsignedString}, p${replicaId.toUnsignedString}, t${timestamp.toUnsignedString})"
}


object LongCuckooEntry {
  /** @see [[probfilter.pdsa.LongCuckooEntry.from]] */
  @inline def parse(fingerprint: Short, replicaId: Short, timestamp: Int): Long = {
    val fp = (fingerprint.toLong & 0xffffL) << 48
    val id = (replicaId.toLong & 0xffffL) << 32
    val ts = timestamp.toLong & 0xffff_ffffL
    fp | id | ts
  }

  @inline def from(long: Long): LongCuckooEntry = new LongCuckooEntry(long)

  @inline def from(fingerprint: Short, replicaId: Short, timestamp: Int): LongCuckooEntry = new LongCuckooEntry(parse(fingerprint, replicaId, timestamp))

  implicit final object Ordering extends PartialOrdering[Long] {
    override def tryCompare(x: Long, y: Long): Option[Int] = if ((x >>> 32) == (y >>> 32)) Some(UnsignedNumber.compare(x, y)) else None

    /**
     * Unlike other [[scala.math.PartialOrdering]], returns a total order. Used only for sorting.
     *
     * @see [[probfilter.pdsa.LongCuckooEntry.Ordering.tryCompare]]
     */
    override def lteq(x: Long, y: Long): Boolean = UnsignedNumber.compare(x, y) <= 0
  }
}
