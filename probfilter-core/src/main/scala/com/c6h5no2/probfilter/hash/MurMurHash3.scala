package com.c6h5no2.probfilter.hash

import com.google.common.hash.Hashing


object MurMurHash3 {
  /** @return 128-bit hash code of `obj` hashed by MurmurHash3_x64_128 */
  def apply[T](obj: T, funnel: Funnel[_ >: T]): Array[Byte] = {
    Hashing.murmur3_128().hashObject(obj, (from, into) => funnel.apply(from, new Sink(into))).asBytes()
  }
}
