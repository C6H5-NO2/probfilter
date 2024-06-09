package com.c6h5no2.probfilter.hash

import com.google.common.primitives.Ints


object FoldHash {
  implicit final class BytesHashCode(private val hash: Array[Byte]) extends AnyVal {
    @inline def xorFoldToInt: Int = {
      require(hash.length == 128 / 8)
      val i3 = Ints.fromBytes(hash.apply(15), hash.apply(14), hash.apply(13), hash.apply(12))
      val i2 = Ints.fromBytes(hash.apply(11), hash.apply(10), hash.apply(9), hash.apply(8))
      val i1 = Ints.fromBytes(hash.apply(7), hash.apply(6), hash.apply(5), hash.apply(4))
      val i0 = Ints.fromBytes(hash.apply(3), hash.apply(2), hash.apply(1), hash.apply(0))
      i0 ^ i1 ^ i2 ^ i3
    }
  }

  implicit final class LongHashCode(private val hash: Long) extends AnyVal {
    @inline def xorFoldToInt: Int = (hash ^ (hash >>> 32)).toInt
  }
}
