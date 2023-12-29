package probfilter.util


/**
 * @note Always `import probfilter.util.UnsignedVal._` to get the correct conversions.
 */
object UnsignedVal {
  implicit final class RichUnsignedByte(private val b: Byte) extends AnyVal {
    @inline def toUnsignedString: String = UnsignedNumber.toString(b)

    @inline def gtu(y: Byte): Boolean = UnsignedNumber.compare(b, y) > 0

    @inline def gtu(y: Short): Boolean = UnsignedNumber.compare(b, y) > 0

    @inline def gtu(y: Int): Boolean = UnsignedNumber.compare(b, y) > 0
  }

  implicit final class RichUnsignedShort(private val s: Short) extends AnyVal {
    @inline def toUnsignedString: String = UnsignedNumber.toString(s)

    @inline def gtu(y: Byte): Boolean = UnsignedNumber.compare(s, y) > 0

    @inline def gtu(y: Short): Boolean = UnsignedNumber.compare(s, y) > 0

    @inline def gtu(y: Int): Boolean = UnsignedNumber.compare(s, y) > 0
  }

  implicit final class RichUnsignedInt(private val i: Int) extends AnyVal {
    @inline def toUnsignedString: String = UnsignedNumber.toString(i)

    @inline def gtu(y: Byte): Boolean = UnsignedNumber.compare(i, y) > 0

    @inline def gtu(y: Short): Boolean = UnsignedNumber.compare(i, y) > 0

    @inline def gtu(y: Int): Boolean = UnsignedNumber.compare(i, y) > 0
  }
}
