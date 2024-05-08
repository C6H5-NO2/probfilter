package probfilter.pdsa.cuckoo

import com.google.common.math.IntMath
import probfilter.hash.FoldHash.{BytesHashCode, LongHashCode}
import probfilter.hash.{FarmHashFingerprint64, Funnel, MurMurHash3}

import scala.util.Try


@SerialVersionUID(1L)
final class SimpleCuckooStrategy[E] private
(val capacity: Int, val numBuckets: Int, val bucketSize: Int,
 val maxIterations: Int, val fingerprintBits: Int, val storageType: EntryStorageType)
(implicit private val funnel: Funnel[_ >: E])
  extends CuckooStrategy[E] {
  override def tighten(): SimpleCuckooStrategy[E] = {
    val newCapacity = capacity + (capacity >>> 1)
    val newFingerprintBits = fingerprintBits + 1
    val newStorageType = storageType match {
      case EntryStorageType.SIMPLE_BYTE =>
        if (fingerprintBits < 8) EntryStorageType.SIMPLE_BYTE else EntryStorageType.SIMPLE_SHORT
      case EntryStorageType.SIMPLE_SHORT =>
        if (fingerprintBits < 16) EntryStorageType.SIMPLE_SHORT else throw new CuckooStrategy.FingerprintLengthExceededException()
      case EntryStorageType.VERSIONED_INT =>
        if (fingerprintBits < 8) EntryStorageType.VERSIONED_INT else EntryStorageType.VERSIONED_LONG
      case EntryStorageType.VERSIONED_LONG =>
        if (fingerprintBits < 16) EntryStorageType.VERSIONED_LONG else throw new CuckooStrategy.FingerprintLengthExceededException()
    }
    SimpleCuckooStrategy.create(newCapacity, bucketSize, maxIterations, newFingerprintBits, newStorageType)
  }

  override def indexHash(elem: E): Int = (MurMurHash3.hash(elem).xorFoldToInt & Int.MaxValue) % numBuckets

  override def fingerprintHash(elem: E): Short = {
    val h = FarmHashFingerprint64.hash(elem).xorFoldToInt & Int.MaxValue
    val m = (1 << fingerprintBits) - 1
    (h % m + 1).toShort
  }

  /** @return `j = i XOR h(f)` */
  override def altIndexOf(i: Int, fp: Short): Int = {
    import probfilter.hash.Funnels.IntFunnel
    import probfilter.util.UnsignedNumber
    val h = MurMurHash3.hash(UnsignedNumber.toUInt(fp))(IntFunnel).xorFoldToInt & Int.MaxValue
    (i ^ h) % numBuckets
  }
}


object SimpleCuckooStrategy {
  /**
   * @param capacity expected number of elements to be inserted
   * @param bucketSize desired number of slots per bucket
   * @param maxIterations maximum number of attempts to cuckoo displace
   * @param fingerprintBits bit length of fingerprint
   * @param storageType `SIMPLE_*` for simple fingerprint; `VERSIONED_*` for versioned entry
   * @param funnel the funnel object to use
   * @throws java.lang.IllegalArgumentException if any argument is illegal
   */
  def create[E](capacity: Int, bucketSize: Int, maxIterations: Int, fingerprintBits: Int, storageType: EntryStorageType)(implicit funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    require(capacity > 0, s"SimpleCuckooStrategy.create: capacity = $capacity not > 0")
    require(1 <= bucketSize && bucketSize <= 8, s"SimpleCuckooStrategy.create: bucketSize = $bucketSize !in [1, 8]")
    val numBuckets = Try.apply(IntMath.ceilingPowerOfTwo(capacity / bucketSize))
    require(numBuckets.isSuccess, s"SimpleCuckooStrategy.create: numBuckets cannot be calculated given capacity $capacity and bucketSize $bucketSize")
    require(maxIterations >= 0, s"SimpleCuckooStrategy.create: maxIterations = $maxIterations < 0")
    val storageTypeErrMsg = () => s"SimpleCuckooStrategy.create: storageType $storageType incompatible with fingerprint of $fingerprintBits bits"
    storageType match {
      case EntryStorageType.SIMPLE_BYTE => require(0 < fingerprintBits && fingerprintBits <= 8, storageTypeErrMsg)
      case EntryStorageType.SIMPLE_SHORT => require(8 < fingerprintBits && fingerprintBits <= 16, storageTypeErrMsg)
      case EntryStorageType.VERSIONED_INT => require(0 < fingerprintBits && fingerprintBits <= 8, storageTypeErrMsg)
      case EntryStorageType.VERSIONED_LONG => require(0 < fingerprintBits && fingerprintBits <= 16, storageTypeErrMsg)
    }
    val fpReqLhs = 2 * bucketSize * fingerprintBits
    val fpReqRhs = math.floor(math.log(capacity) / math.log(2))
    require(fpReqLhs >= fpReqRhs, s"SimpleCuckooStrategy.create: expect 4 ^ (bucketSize * fingerprintBits) > \\Omega(capacity), but $fpReqLhs < $fpReqRhs")
    new SimpleCuckooStrategy[E](capacity, numBuckets.get, bucketSize, maxIterations, fingerprintBits, storageType)(funnel)
  }

  def create[E](capacity: Int, bucketSize: Int, maxIterations: Int)(implicit funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    create(capacity, bucketSize, maxIterations, 8, EntryStorageType.SIMPLE_BYTE)
  }

  def create[E](capacity: Int, bucketSize: Int, maxIterations: Int, fingerprintBits: Int)(implicit funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    val storageType = if (fingerprintBits <= 8) EntryStorageType.SIMPLE_BYTE else EntryStorageType.SIMPLE_SHORT
    create(capacity, bucketSize, maxIterations, fingerprintBits, storageType)
  }

  def create[E](capacity: Int, bucketSize: Int, maxIterations: Int, storageType: EntryStorageType)(implicit funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    val fingerprintBits = storageType match {
      case EntryStorageType.SIMPLE_BYTE => 8
      case EntryStorageType.SIMPLE_SHORT => 16
      case EntryStorageType.VERSIONED_INT => 8
      case EntryStorageType.VERSIONED_LONG => 8
    }
    create(capacity, bucketSize, maxIterations, fingerprintBits, storageType)
  }
}
