package probfilter.pdsa

import com.google.common.hash.PrimitiveSink

import java.nio.ByteBuffer
import java.nio.charset.Charset


//noinspection UnstableApiUsage
class Sink private[probfilter](private val sink: PrimitiveSink) extends PrimitiveSink {
  override def putByte(b: Byte): Sink = {sink.putByte(b); this}

  override def putBytes(bytes: Array[Byte]): Sink = {sink.putBytes(bytes); this}

  override def putBytes(bytes: Array[Byte], off: Int, len: Int): Sink = {sink.putBytes(bytes, off, len); this}

  override def putBytes(bytes: ByteBuffer): Sink = {sink.putBytes(bytes); this}

  override def putShort(s: Short): Sink = {sink.putShort(s); this}

  override def putInt(i: Int): Sink = {sink.putInt(i); this}

  override def putLong(l: Long): Sink = {sink.putLong(l); this}

  override def putFloat(f: Float): Sink = {sink.putFloat(f); this}

  override def putDouble(d: Double): Sink = {sink.putDouble(d); this}

  override def putBoolean(b: Boolean): Sink = {sink.putBoolean(b); this}

  override def putChar(c: Char): Sink = {sink.putChar(c); this}

  override def putUnencodedChars(charSequence: CharSequence): Sink = {sink.putUnencodedChars(charSequence); this}

  override def putString(charSequence: CharSequence, charset: Charset): Sink = {sink.putString(charSequence, charset); this}
}
