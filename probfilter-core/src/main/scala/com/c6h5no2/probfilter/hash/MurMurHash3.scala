package probfilter.hash

import com.google.common.hash.{Hashing, PrimitiveSink}


object MurMurHash3 {
  /** @return 128-bit hash code of `obj` hashed by MurmurHash3_x64_128 */
  def hash[T](obj: T)(implicit funnel: Funnel[_ >: T]): Array[Byte] = {
    //noinspection UnstableApiUsage
    Hashing.murmur3_128().hashObject(obj, (from: T, into: PrimitiveSink) => {
      val sink = new Sink(into)
      funnel.funnel(from, sink)
    }).asBytes()
  }
}
