package probfilter.pdsa

import com.google.common.math.IntMath
import probfilter.hash.{FarmHashFingerprint64, Funnel, MurMurHash3}
import probfilter.util.UnsignedNumber


@SerialVersionUID(1L)
class CuckooStrategy[T] private
(val numBuckets: Int, val bucketSize: Int, val maxIterations: Int, val capacity: Int, val fpBits: Int)
(implicit val funnel: Funnel[_ >: T]) extends Serializable {
  val fpp: Double = 2.0 * bucketSize / (1 << fpBits)

  /** @return `fp = fingerprint(elem); i = hash(elem)` */
  def getCuckooPair(elem: T): CuckooStrategy.Pair = {
    // 1 <= fp <= (... 1111 1111)
    val fp = (FarmHashFingerprint64.hash(elem) % ((1 << fpBits) - 1) + 1).toShort
    val i = (MurMurHash3.hash(elem) & Int.MaxValue).toInt % numBuckets
    new CuckooStrategy.Pair(fp, i)
  }

  /** @return `j = i ^ hash(fp)` */
  def getAltBucket(fp: Short, i: Int): Int = {
    import probfilter.hash.Funnels.IntFunnel
    val h = (MurMurHash3.hash(UnsignedNumber.toUInt(fp))(IntFunnel) & Int.MaxValue).toInt
    (i ^ h) % numBuckets
  }

  def getCuckooTriple(elem: T): CuckooStrategy.Triple = {
    val pair = getCuckooPair(elem)
    new CuckooStrategy.Triple(pair.fp, pair.i, getAltBucket(pair.fp, pair.i))
  }
}


object CuckooStrategy {
  /**
   * @param capacity expected number of elements to be inserted
   * @param bucketSize desired number of slots per bucket
   * @param maxIterations maximum number of attempts to cuckoo
   * @param funnel the funnel object to use
   * @throws IllegalArgumentException if any parameter is out of range
   */
  def create[T](capacity: Int, bucketSize: Int, maxIterations: Int)(implicit funnel: Funnel[_ >: T]): CuckooStrategy[T] = {
    require(capacity > 0, s"CuckooStrategy.create: capacity = $capacity not > 0")
    require(1 <= bucketSize && bucketSize <= 8, s"CuckooStrategy.create: bucketSize = $bucketSize !in [1, 8]")
    require(maxIterations >= 0, s"CuckooStrategy.create: maxIterations = $maxIterations < 0")
    val numBuckets = try {
      IntMath.ceilingPowerOfTwo(capacity / bucketSize)
    } catch {
      case _: IllegalArgumentException | _: ArithmeticException =>
        throw new IllegalArgumentException(s"CuckooStrategy.create: capacity/bucketSize = ${capacity / bucketSize}")
    }
    new CuckooStrategy[T](numBuckets, bucketSize, maxIterations, capacity, 8)(funnel)
  }

  final class Pair(val fp: Short, val i: Int)

  final class Triple(val fp: Short, val i: Int, val j: Int) {
    override def equals(obj: Any): Boolean = obj match {
      case obj: Triple => fp == obj.fp && i == obj.i && j == obj.j
      case Tuple3(_1: Int, _2: Int, _3: Int) => UnsignedNumber.toUInt(fp) == _1 && i == _2 && j == _3
      case _ => false
    }

    override def hashCode(): Int = java.util.Objects.hash(fp, i, j)

    override def toString: String = s"(${UnsignedNumber.toString(fp)}, $i, $j)"
  }

  final class BucketOverflowException(private val elem: Any, private val i: Int)
    extends RuntimeException(s"Found overflowed bucket at $i when trying to add $elem")

  final class MaxIterationReachedException(private val elem: Any)
    extends RuntimeException(s"Reached maximum number of iterations when trying to add $elem")

  final class MaxFingerprintLengthReachedException(private val elem: Any)
    extends RuntimeException(s"Reached current maximum length of fingerprint (16 bits) when trying to add $elem")
}
