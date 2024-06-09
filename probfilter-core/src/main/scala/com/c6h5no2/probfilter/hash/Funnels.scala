package com.c6h5no2.probfilter.hash

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.nio.charset.StandardCharsets


/** Common [[Funnel]]s. */
object Funnels {
  /**
   * [[Funnel]] for Scala's [[scala.Int]].
   *
   * @see [[Funnels.IntegerFunnel]]
   */
  @SerialVersionUID(1L)
  object IntFunnel extends Funnel[Int] {
    override def apply(from: Int, into: Sink): Unit = into.putInt(from)
  }

  /**
   * [[Funnel]] for Java's [[java.lang.Integer]].
   *
   * @see [[Funnels.IntFunnel]], [[Funnels.getIntegerFunnel]]
   */
  @SerialVersionUID(1L)
  private object IntegerFunnel extends Funnel[Integer] {
    override def apply(from: Integer, into: Sink): Unit = into.putInt(from)
  }

  /** [[Funnel]] for UTF-8 [[java.lang.String]]. */
  @SerialVersionUID(1L)
  object StringFunnel extends Funnel[String] {
    override def apply(from: String, into: Sink): Unit = into.putString(from, StandardCharsets.UTF_8)
  }

  /** [[Funnel]] for [[java.io.Serializable]]. */
  @SerialVersionUID(1L)
  object DefaultFunnel extends Funnel[Object] {
    override def apply(from: Object, into: Sink): Unit = {
      val bos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(bos)
      oos.writeObject(from)
      oos.flush()
      oos.close()
      val bytes = bos.toByteArray
      into.putBytes(bytes)
    }
  }

  def getIntegerFunnel: Funnel[Integer] = IntegerFunnel

  def getStringFunnel: Funnel[String] = StringFunnel

  def getDefaultFunnel: Funnel[Object] = DefaultFunnel
}
