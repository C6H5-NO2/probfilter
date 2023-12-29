package probfilter.pdsa

import probfilter.util.UnsignedVal._
import probfilter.util.{JavaFriendly, UnsignedNumber}

import java.util.OptionalInt


/**
 * An immutable wrapper of 64-bit unsigned long representing an entry in the cuckoo table.
 * This class is also a value class which avoids runtime object allocation.
 */
final class CuckooEntry(private val data: Long) extends AnyVal {
  /**
   * @return 8-bit unsigned fingerprint
   */
  @inline def fingerprint: Byte = ((data >>> 48) & 0xffL).toByte

  /**
   * @return 16-bit unsigned replica id
   */
  @inline def replicaId: Short = ((data >>> 32) & 0xffffL).toShort

  /**
   * @return 32-bit unsigned timestamp
   */
  @inline def timestamp: Int = (data & 0xffff_ffffL).toInt

  @inline def tryCompareTo(that: CuckooEntry): Option[Int] = CuckooEntry.Ordering.tryCompare(data, that.toLong)

  @JavaFriendly(scalaDelegate = "tryCompareTo")
  def tryCompareToAsJava(that: CuckooEntry): OptionalInt = {
    import scala.jdk.OptionConverters.RichOption
    tryCompareTo(that).toJavaPrimitive[OptionalInt]
  }

  /**
   * @return `true` if `this` is partially less than `that`; `false` otherwise
   */
  @inline def lt(that: CuckooEntry): Boolean =
    ((data >>> 32) == (that.toLong >>> 32)) && (UnsignedNumber.compare(data, that.toLong) < 0)

  /**
   * @return `true` if `this` is partially greater than `that`; `false` otherwise
   */
  @inline def gt(that: CuckooEntry): Boolean =
    ((data >>> 32) == (that.toLong >>> 32)) && (UnsignedNumber.compare(data, that.toLong) > 0)

  @inline def toLong: Long = data

  override def toString: String =
    s"E(f${fingerprint.toUnsignedString}, p${replicaId.toUnsignedString}, t${timestamp.toUnsignedString})"
}


object CuckooEntry {
  /**
   * @see [[probfilter.pdsa.CuckooEntry.of]]
   */
  @inline def parse(fingerprint: Byte, replicaId: Short, timestamp: Int): Long = {
    val fp = (fingerprint.toLong & 0xffL) << 48
    val id = (replicaId.toLong & 0xffffL) << 32
    val ts = timestamp.toLong & 0xffff_ffffL
    fp | id | ts
  }

  @inline def of(fingerprint: Byte, replicaId: Short, timestamp: Int): CuckooEntry = {
    new CuckooEntry(parse(fingerprint, replicaId, timestamp))
  }

  @inline def of(long: Long): CuckooEntry = {
    new CuckooEntry(long)
  }

  implicit object Ordering extends PartialOrdering[Long] {
    override def tryCompare(x: Long, y: Long): Option[Int] = {
      if ((x >>> 32) == (y >>> 32))
        Some(UnsignedNumber.compare(x, y))
      else
        None
    }

    /**
     * Unlike other [[scala.math.PartialOrdering]], returns a total order. Used only for sorting.
     *
     * @see [[probfilter.pdsa.CuckooEntry.Ordering.tryCompare]]
     */
    override def lteq(x: Long, y: Long): Boolean = {
      UnsignedNumber.compare(x, y) <= 0
    }
  }
}
