package com.c6h5no2.probfilter.hash

import com.google.common.hash.Hashing


object FarmHashFingerprint64 {
  /** @return 64-bit hash code of `obj` hashed by FarmHash Fingerprint64 */
  def apply[T](obj: T, funnel: Funnel[_ >: T]): Long = {
    Hashing.farmHashFingerprint64().hashObject(obj, (from, into) => funnel.apply(from, new Sink(into))).asLong()
  }
}
