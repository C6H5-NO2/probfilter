package probfilter.pdsa

import com.google.common.math.IntMath
import probfilter.hash.{Funnel, MurmurHash3}
import probfilter.util.UnsignedNumber


@SerialVersionUID(1L)
class CuckooStrategy[T] private(val numBuckets: Int, val bucketSize: Int, val maxIterations: Int)
                               (implicit val funnel: Funnel[_ >: T]) extends Serializable {
  /**
   * @return `fp = fingerprint(elem); i = hash(elem)`
   */
  def getCuckooPair(elem: T): CuckooStrategy.Pair = {
    val hash = MurmurHash3.hash(elem)
    val fp = ((hash >>> 32) % 255 + 1).toByte
    val i = (hash & Int.MaxValue).toInt % numBuckets
    new CuckooStrategy.Pair(fp, i)
  }

  /**
   * @return `j = i ^ hash(fp)`
   */
  def getAltBucket(fp: Byte, i: Int): Int = {
    import probfilter.hash.Funnels.IntFunnel
    val h = (MurmurHash3.hash(UnsignedNumber.toUInt(fp)) & Int.MaxValue).toInt
    (i ^ h) % numBuckets
  }

  def getCuckooTriple(elem: T): CuckooStrategy.Triple = {
    val pair = getCuckooPair(elem)
    new CuckooStrategy.Triple(pair.fp, pair.i, getAltBucket(pair.fp, pair.i))
  }

  def iterator(i: Int): CuckooStrategy.PeekingIterator = {
    new CuckooStrategy.PeekingIterator(i, this) {}
  }
}


object CuckooStrategy {
  final class Pair(val fp: Byte, val i: Int)
  final class Triple(val fp: Byte, val i: Int, val j: Int)


  /**
   * An iterator that supports `peek`ing the current element.
   */
  sealed abstract class PeekingIterator(private var idx: Int, private val strategy: CuckooStrategy[_]) {
    private var iter = 0

    def peek: Int = idx

    def hasNext: Boolean = iter < strategy.maxIterations

    def next(fp: Byte): Int = {
      iter += 1
      idx = strategy.getAltBucket(fp, idx)
      idx
    }
  }


  final class BucketOverflowException(private val elem: Any, private val i: Int)
    extends RuntimeException(s"Found overflowed bucket at $i when trying to add $elem")


  final class MaxIterationReachedException(private val elem: Any)
    extends RuntimeException(s"Reached maximum number of iterations when trying to add $elem")


  /**
   * @param capacity expected number of elements to be inserted
   * @param bucketSize number of slots per bucket
   * @param maxIterations maximum number of attempts to cuckoo
   * @param funnel the funnel object to use
   * @throws IllegalArgumentException if any parameter is out of range
   */
  def create[T](capacity: Int, bucketSize: Int, maxIterations: Int)
               (implicit funnel: Funnel[_ >: T]): CuckooStrategy[T] = {
    require(capacity > 0, s"CuckooStrategy.create: capacity = $capacity not > 0")
    require((1 to 4) contains bucketSize, s"CuckooStrategy.create: bucketSize = $bucketSize !in {1, 2, 3, 4}")
    require(maxIterations >= 0, s"CuckooStrategy.create: maxIterations = $maxIterations < 0")
    val numBuckets = try {
      IntMath.ceilingPowerOfTwo(capacity / bucketSize)
    } catch {
      case _: IllegalArgumentException | _: ArithmeticException =>
        throw new IllegalArgumentException(s"CuckooStrategy.create: capacity/bucketSize = ${capacity / bucketSize}")
    }
    new CuckooStrategy[T](numBuckets, bucketSize, maxIterations)(funnel)
  }
}
