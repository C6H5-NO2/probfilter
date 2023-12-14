package probfilter.pdsa

import com.google.common.hash.PrimitiveSink

import java.nio.ByteBuffer
import java.nio.charset.Charset


//noinspection UnstableApiUsage
class Sink private[probfilter](private val sink: PrimitiveSink) {
  def putByte(b: Byte): Sink = {sink.putByte(b); this}

  def putBytes(bytes: Array[Byte]): Sink = {sink.putBytes(bytes); this}

  def putBytes(bytes: Array[Byte], off: Int, len: Int): Sink = {sink.putBytes(bytes, off, len); this}

  def putBytes(bytes: ByteBuffer): Sink = {sink.putBytes(bytes); this}

  def putShort(s: Short): Sink = {sink.putShort(s); this}

  def putInt(i: Int): Sink = {sink.putInt(i); this}

  def putLong(l: Long): Sink = {sink.putLong(l); this}

  def putFloat(f: Float): Sink = {sink.putFloat(f); this}

  def putDouble(d: Double): Sink = {sink.putDouble(d); this}

  def putBoolean(b: Boolean): Sink = {sink.putBoolean(b); this}

  def putChar(c: Char): Sink = {sink.putChar(c); this}

  def putUnencodedChars(charSequence: CharSequence): Sink = {sink.putUnencodedChars(charSequence); this}

  def putString(charSequence: CharSequence, charset: Charset): Sink = {sink.putString(charSequence, charset); this}
}
