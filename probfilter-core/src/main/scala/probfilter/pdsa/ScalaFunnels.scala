package probfilter.pdsa

import java.nio.charset.StandardCharsets


object ScalaFunnels {
  implicit case object StringFunnel extends Funnel[String] {
    override def funnel(from: String, into: Sink): Unit = into.putString(from, StandardCharsets.UTF_8)
  }

  implicit case object IntFunnel extends Funnel[Int] {
    override def funnel(from: Int, into: Sink): Unit = into.putInt(from)
  }

  implicit case object ByteFunnel extends Funnel[Byte] {
    override def funnel(from: Byte, into: Sink): Unit = into.putByte(from)
  }
}
