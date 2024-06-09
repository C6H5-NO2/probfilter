package probfilter.hash

import probfilter.util.JavaFriendly

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.nio.charset.StandardCharsets


/** Common [[probfilter.hash.Funnel]]s. */
object Funnels {
  @SerialVersionUID(1L)
  implicit final object StringFunnel extends Funnel[String] {
    override def funnel(from: String, into: Sink): Unit = into.putString(from, StandardCharsets.UTF_8)
  }

  @SerialVersionUID(1L)
  implicit final object IntFunnel extends Funnel[Int] {
    override def funnel(from: Int, into: Sink): Unit = into.putInt(from)
  }

  @SerialVersionUID(1L)
  private final object IntegerFunnel extends Funnel[Integer] {
    override def funnel(from: Integer, into: Sink): Unit = into.putInt(from)
  }

  @SerialVersionUID(1L)
  /* implicit */ final object DefaultFunnel extends Funnel[Object] {
    override def funnel(from: Object, into: Sink): Unit = {
      val bos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(bos)
      oos.writeObject(from)
      oos.close()
      into.putBytes(bos.toByteArray)
    }
  }

  @JavaFriendly(scalaDelegate = "probfilter.hash.Funnels.StringFunnel")
  def getStringFunnel: Funnel[String] = StringFunnel

  @JavaFriendly(scalaDelegate = "probfilter.hash.Funnels.IntFunnel")
  def getIntegerFunnel: Funnel[Integer] = IntegerFunnel

  @JavaFriendly(scalaDelegate = "probfilter.hash.Funnels.DefaultFunnel")
  def getDefaultFunnel: Funnel[Object] = DefaultFunnel
}
