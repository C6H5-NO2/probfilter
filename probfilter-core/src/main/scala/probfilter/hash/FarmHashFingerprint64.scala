package probfilter.hash

import com.google.common.hash.{Hashing, PrimitiveSink}


@SerialVersionUID(1L)
final class FarmHashFingerprint64[T](implicit private val funnel: Funnel[_ >: T]) extends Serializable {
  def hash(obj: T): Long = FarmHashFingerprint64.hash(obj)
}


object FarmHashFingerprint64 {
  /** @return hash code of `obj` hashed by FarmHash Fingerprint64 */
  def hash[T](obj: T)(implicit funnel: Funnel[_ >: T]): Long = {
    //noinspection UnstableApiUsage
    Hashing.farmHashFingerprint64().hashObject(obj, (from: T, into: PrimitiveSink) => {
      val sink = new Sink(into)
      funnel.funnel(from, sink)
    }).asLong()
  }
}
