package probfilter.hash

import com.google.common.hash.{Hashing, PrimitiveSink}


object FarmHashFingerprint64 {
  /** @return 64-bit hash code of `obj` hashed by FarmHash Fingerprint64 */
  def hash[T](obj: T)(implicit funnel: Funnel[_ >: T]): Long = {
    //noinspection UnstableApiUsage
    Hashing.farmHashFingerprint64().hashObject(obj, (from: T, into: PrimitiveSink) => {
      val sink = new Sink(into)
      funnel.funnel(from, sink)
    }).asLong()
  }
}
