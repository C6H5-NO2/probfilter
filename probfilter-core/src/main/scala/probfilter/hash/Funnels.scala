package probfilter.hash

import probfilter.util.JavaFriendly

import java.nio.charset.StandardCharsets


object Funnels {
  implicit object StringFunnel extends Funnel[String] {
    override def funnel(from: String, into: Sink): Unit = into.putString(from, StandardCharsets.UTF_8)
  }

  implicit object IntFunnel extends Funnel[Int] {
    override def funnel(from: Int, into: Sink): Unit = into.putInt(from)
  }

  private object IntegerFunnel extends Funnel[Integer] {
    override def funnel(from: Integer, into: Sink): Unit = into.putInt(from)
  }

  @JavaFriendly("StringFunnel")
  def stringFunnel(): Funnel[String] = StringFunnel

  @JavaFriendly("IntFunnel")
  def integerFunnel(): Funnel[Integer] = IntegerFunnel
}
