package probfilter.util


/** @note Always `import probfilter.util.UnsignedVal._` to import all conversions. */
object UnsignedVal {
  implicit final class RichUnsignedByte(private val b: Byte) extends AnyVal {
    @inline def toUString: String = UnsignedNumber.toString(b)
  }

  implicit final class RichUnsignedShort(private val s: Short) extends AnyVal {
    @inline def toUString: String = UnsignedNumber.toString(s)
  }

  implicit final class RichUnsignedInt(private val i: Int) extends AnyVal {
    @inline def toUString: String = UnsignedNumber.toString(i)
  }
}
