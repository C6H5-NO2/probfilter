package com.c6h5no2.probfilter.pdsa.cuckoo

import com.c6h5no2.probfilter.hash.FoldHash.{BytesHashCode, LongHashCode}
import com.c6h5no2.probfilter.hash.{FarmHashFingerprint64, Funnel, MurMurHash3}
import com.google.common.math.IntMath

import scala.util.Try


@SerialVersionUID(1L)
final class SimpleCuckooStrategy[E] private(
  val capacity: Int,
  val numBuckets: Int,
  val bucketSize: Int,
  val maxIterations: Int,
  val fingerprintBits: Int,
  val entryType: CuckooEntryType,
  funnel: Funnel[_ >: E],
) extends CuckooStrategy[E] {
  override def tighten(): SimpleCuckooStrategy[E] = {
    val newCapacity = capacity // + (capacity >>> 1)
    val newFingerprintBits = fingerprintBits + 1
    val newEntryType = entryType match {
      case CuckooEntryType.SIMPLE_BYTE =>
        if (fingerprintBits < 8) CuckooEntryType.SIMPLE_BYTE
        else CuckooEntryType.SIMPLE_SHORT
      case CuckooEntryType.SIMPLE_SHORT =>
        if (fingerprintBits < 16) CuckooEntryType.SIMPLE_SHORT
        else throw new CuckooStrategy.FingerprintLengthExceededException()
      case CuckooEntryType.VERSIONED_INT =>
        if (fingerprintBits < 8) CuckooEntryType.VERSIONED_INT
        else CuckooEntryType.VERSIONED_LONG
      case CuckooEntryType.VERSIONED_LONG =>
        if (fingerprintBits < 16) CuckooEntryType.VERSIONED_LONG
        else throw new CuckooStrategy.FingerprintLengthExceededException()
    }
    SimpleCuckooStrategy.apply(newCapacity, bucketSize, maxIterations, newFingerprintBits, newEntryType, funnel)
  }

  override def indexHash(elem: E): Int = {
    (MurMurHash3.apply(elem, funnel).xorFoldToInt & Int.MaxValue) % numBuckets
  }

  override def fingerprintHash(elem: E): Short = {
    val h = FarmHashFingerprint64.apply(elem, funnel).xorFoldToInt & Int.MaxValue
    val m = (1 << fingerprintBits) - 1
    (h % m + 1).toShort
  }

  /** @return `j = i XOR h(f)` */
  override def altIndexOf(i: Int, fp: Short): Int = {
    import com.c6h5no2.probfilter.hash.Funnels.IntFunnel
    import com.c6h5no2.probfilter.util.UnsignedNumber
    val h = MurMurHash3.apply(UnsignedNumber.toUInt(fp), IntFunnel).xorFoldToInt & Int.MaxValue
    (i ^ h) % numBuckets
  }
}

object SimpleCuckooStrategy {
  /**
   * @param capacity expected number of elements to be inserted
   * @param bucketSize desired number of slots per bucket
   * @param maxIterations maximum number of attempts to cuckoo displace
   * @param fingerprintBits bit length of fingerprint
   * @param entryType `CuckooEntryType.SIMPLE_*` for simple fingerprint; `CuckooEntryType.VERSIONED_*` for versioned entry
   * @param funnel the funnel object to use
   * @throws java.lang.IllegalArgumentException if any argument is illegal
   */
  def apply[E](
    capacity: Int,
    bucketSize: Int,
    maxIterations: Int,
    fingerprintBits: Int,
    entryType: CuckooEntryType,
    funnel: Funnel[_ >: E],
  ): SimpleCuckooStrategy[E] = {
    require(capacity > 0, s"${getClass.getName}.apply: capacity = $capacity not > 0")
    require(1 <= bucketSize && bucketSize <= 16, s"${getClass.getName}.apply: bucketSize = $bucketSize !in [1, 16]")
    val numBuckets = Try.apply(IntMath.ceilingPowerOfTwo(capacity / bucketSize))
    require(numBuckets.isSuccess, s"${getClass.getName}.apply: numBuckets cannot be calculated given capacity $capacity and bucketSize $bucketSize")
    require(maxIterations >= 0, s"${getClass.getName}.apply: maxIterations = $maxIterations < 0")
    val entryTypeErrMsg = () => s"${getClass.getName}.apply: entryType $entryType incompatible with fingerprint of $fingerprintBits bits"
    entryType match {
      case CuckooEntryType.SIMPLE_BYTE => require(0 < fingerprintBits && fingerprintBits <= 8, entryTypeErrMsg)
      case CuckooEntryType.SIMPLE_SHORT => require(0 < fingerprintBits && fingerprintBits <= 16, entryTypeErrMsg)
      case CuckooEntryType.VERSIONED_INT => require(0 < fingerprintBits && fingerprintBits <= 8, entryTypeErrMsg)
      case CuckooEntryType.VERSIONED_LONG => require(0 < fingerprintBits && fingerprintBits <= 16, entryTypeErrMsg)
    }
    val fpReqLhs = 2 * bucketSize * fingerprintBits
    val fpReqRhs = math.floor(math.log(capacity) / math.log(2))
    require(fpReqLhs >= fpReqRhs, s"${getClass.getName}.apply: expect 4 ^ (bucketSize * fingerprintBits) > \\Omega(capacity), but $fpReqLhs < $fpReqRhs")
    new SimpleCuckooStrategy[E](capacity, numBuckets.get, bucketSize, maxIterations, fingerprintBits, entryType, funnel)
  }

  /** `fingerprintBits` defaults to the maximum possible value, i.e. 8 or 16. */
  def apply[E](capacity: Int, bucketSize: Int, maxIterations: Int, entryType: CuckooEntryType, funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    val fingerprintBits = entryType match {
      case CuckooEntryType.SIMPLE_BYTE => 8
      case CuckooEntryType.SIMPLE_SHORT => 16
      case CuckooEntryType.VERSIONED_INT => 8
      case CuckooEntryType.VERSIONED_LONG => 16
    }
    apply[E](capacity, bucketSize, maxIterations, fingerprintBits, entryType, funnel)
  }

  /** `entryType` defaults to `CuckooEntryType.SIMPLE_*`. */
  def apply[E](capacity: Int, bucketSize: Int, maxIterations: Int, fingerprintBits: Int, funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    val entryType = if (fingerprintBits <= 8) CuckooEntryType.SIMPLE_BYTE else CuckooEntryType.SIMPLE_SHORT
    apply[E](capacity, bucketSize, maxIterations, fingerprintBits, entryType, funnel)
  }

  /** `fingerprintBits` defaults to 8; `entryType` defaults to `CuckooEntryType.SIMPLE_BYTE`. */
  def apply[E](capacity: Int, bucketSize: Int, maxIterations: Int, funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    apply[E](capacity, bucketSize, maxIterations, 8, CuckooEntryType.SIMPLE_BYTE, funnel)
  }
}
