package probfilter.hash

import com.google.common.hash.{Hashing, PrimitiveSink}


object MurmurHash3 {
  def hash[T](obj: T)(implicit funnel: Funnel[_ >: T]): Long = {
    //noinspection UnstableApiUsage
    Hashing.murmur3_128().hashObject(obj, (from: T, into: PrimitiveSink) => {
      val sink = new Sink(into)
      funnel.funnel(from, sink)
    }).asLong()
  }
}