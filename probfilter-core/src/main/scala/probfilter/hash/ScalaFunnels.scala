package probfilter.hash

import java.nio.charset.StandardCharsets


object ScalaFunnels {
  implicit object StringFunnel extends Funnel[String] {
    override def funnel(from: String, into: Sink): Unit = into.putString(from, StandardCharsets.UTF_8)
  }

  implicit object IntFunnel extends Funnel[Int] {
    override def funnel(from: Int, into: Sink): Unit = into.putInt(from)
  }
}
