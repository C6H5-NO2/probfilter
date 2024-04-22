package probfilter.util


/** @note Always `import probfilter.util.UnsignedVal._` to import all conversions. */
object UnsignedVal {
  implicit final class RichUnsignedByte(private val b: Byte) extends AnyVal {
    @inline def toUString: String = UnsignedNumber.toString(b)

    /** @return `true` if greater than unsigned */
    @inline def gtu(y: Byte): Boolean = UnsignedNumber.compare(b, y) > 0

    /** @return `true` if greater than unsigned */
    @inline def gtu(y: Short): Boolean = UnsignedNumber.compare(b, y) > 0

    /** @return `true` if greater than unsigned */
    @inline def gtu(y: Int): Boolean = UnsignedNumber.compare(b, y) > 0
  }

  implicit final class RichUnsignedShort(private val s: Short) extends AnyVal {
    @inline def toUString: String = UnsignedNumber.toString(s)

    /** @return `true` if greater than unsigned */
    @inline def gtu(y: Byte): Boolean = UnsignedNumber.compare(s, y) > 0

    /** @return `true` if greater than unsigned */
    @inline def gtu(y: Short): Boolean = UnsignedNumber.compare(s, y) > 0

    /** @return `true` if greater than unsigned */
    @inline def gtu(y: Int): Boolean = UnsignedNumber.compare(s, y) > 0
  }

  implicit final class RichUnsignedInt(private val i: Int) extends AnyVal {
    @inline def toUString: String = UnsignedNumber.toString(i)

    /** @return `true` if greater than unsigned */
    @inline def gtu(y: Byte): Boolean = UnsignedNumber.compare(i, y) > 0

    /** @return `true` if greater than unsigned */
    @inline def gtu(y: Short): Boolean = UnsignedNumber.compare(i, y) > 0

    /** @return `true` if greater than unsigned */
    @inline def gtu(y: Int): Boolean = UnsignedNumber.compare(i, y) > 0
  }
}
