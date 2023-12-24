package probfilter.crdt

import probfilter.util.UnsignedVal._
import probfilter.util.{JavaFriendly, UnsignedNumber}

import java.util.OptionalInt
import scala.jdk.OptionConverters.RichOption


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

  @inline def tryCompareTo(that: CuckooEntry): Option[Int] = CuckooEntry.Ordering.tryCompare(data, that.data)

  @JavaFriendly("tryCompareTo")
  def tryCompareToAsJava(that: CuckooEntry): OptionalInt = tryCompareTo(that).toJavaPrimitive[OptionalInt]

  @inline def lt(that: CuckooEntry): Boolean = tryCompareTo(that).getOrElse(1) < 0

  override def toString: String =
    s"E(f${fingerprint.toUnsignedString},p${replicaId.toUnsignedString},t${timestamp.toUnsignedString})"
}


object CuckooEntry {
  def apply(long: Long): CuckooEntry = new CuckooEntry(long)

  /**
   * @see [[probfilter.crdt.CuckooEntry.from]]
   */
  def parse(fingerprint: Byte, replicaId: Short, timestamp: Int): Long = {
    val fp = (fingerprint.toLong & 0xffL) << 48
    val id = (replicaId.toLong & 0xffffL) << 32
    val ts = timestamp.toLong & 0xffff_ffffL
    fp | id | ts
  }

  def from(fingerprint: Byte, replicaId: Short, timestamp: Int): CuckooEntry = {
    new CuckooEntry(parse(fingerprint, replicaId, timestamp))
  }

  implicit object Ordering extends PartialOrdering[Long] {
    override def tryCompare(x: Long, y: Long): Option[Int] = {
      if ((x >>> 32) == (y >>> 32))
        Some(UnsignedNumber.compare(x, y))
      else
        None
    }

    /**
     * Unlike other [[scala.math.PartialOrdering]], returns a total order. Use it only for sorting.
     *
     * @see [[probfilter.crdt.CuckooEntry.Ordering.tryCompare]]
     */
    override def lteq(x: Long, y: Long): Boolean = {
      UnsignedNumber.compare(x, y) <= 0
    }
  }
}
