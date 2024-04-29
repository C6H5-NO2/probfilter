package probfilter.pdsa.cuckoo

import com.google.common.math.IntMath
import probfilter.hash.{FarmHashFingerprint64, Funnel, MurMurHash3}

import scala.util.Try


@SerialVersionUID(1L)
final class SimpleCuckooStrategy[E] private[cuckoo]
(val capacity: Int, val numBuckets: Int, val bucketSize: Int,
 val maxIterations: Int, val fingerprintBits: Int, val storageType: EntryStorageType)
(implicit private val funnel: Funnel[_ >: E])
  extends CuckooStrategy[E] {
  override def tighten(): SimpleCuckooStrategy[E] = {
    val newCapacity = capacity + (capacity >>> 1)
    val newFingerprintBits = fingerprintBits + 1
    val newStorageType = storageType match {
      case EntryStorageType.BYTE =>
        if (fingerprintBits < 8) EntryStorageType.BYTE else EntryStorageType.SHORT
      case EntryStorageType.SHORT =>
        if (fingerprintBits < 16) EntryStorageType.SHORT else throw new CuckooStrategy.FingerprintLengthExceededException()
      case EntryStorageType.INT =>
        if (fingerprintBits < 8) EntryStorageType.INT else EntryStorageType.LONG
      case EntryStorageType.LONG =>
        if (fingerprintBits < 16) EntryStorageType.LONG else throw new CuckooStrategy.FingerprintLengthExceededException()
    }
    SimpleCuckooStrategy.create(newCapacity, bucketSize, maxIterations, newFingerprintBits, newStorageType)
  }

  override def indexHash(elem: E): Int = (MurMurHash3.hash(elem) & Int.MaxValue).toInt % numBuckets

  override def fingerprintHash(elem: E): Short = {
    val h = FarmHashFingerprint64.hash(elem) & Int.MaxValue
    val m = (1 << fingerprintBits) - 1
    (h % m + 1).toShort
  }

  /** @return `j = i XOR h(f)` */
  override def altIndexOf(i: Int, fp: Short): Int = {
    import probfilter.hash.Funnels.IntFunnel
    import probfilter.util.UnsignedNumber
    val h = (MurMurHash3.hash(UnsignedNumber.toUInt(fp))(IntFunnel) & Int.MaxValue).toInt
    (i ^ h) % numBuckets
  }
}


object SimpleCuckooStrategy {
  /**
   * @param capacity expected number of elements to be inserted
   * @param bucketSize desired number of slots per bucket
   * @param maxIterations maximum number of attempts to cuckoo displace
   * @param fingerprintBits bit length of fingerprint
   * @param storageType `BYTE`/`SHORT` for simple fingerprint; `LONG` for versioned entry
   * @param funnel the funnel object to use
   * @throws java.lang.IllegalArgumentException if any argument is illegal
   */
  def create[E](capacity: Int, bucketSize: Int, maxIterations: Int, fingerprintBits: Int, storageType: EntryStorageType)
               (implicit funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    require(capacity > 0, s"SimpleCuckooStrategy.create: capacity = $capacity not > 0")
    require(1 <= bucketSize && bucketSize <= 8, s"SimpleCuckooStrategy.create: bucketSize = $bucketSize !in [1, 8]")
    val numBuckets = Try.apply(IntMath.ceilingPowerOfTwo(capacity / bucketSize))
    require(numBuckets.isSuccess, s"SimpleCuckooStrategy.create: numBuckets = ${capacity / bucketSize}")
    require(maxIterations >= 0, s"SimpleCuckooStrategy.create: maxIterations = $maxIterations < 0")
    val storageTypeErrMsg = () => s"SimpleCuckooStrategy.create: storageType $storageType incompatible with $fingerprintBits bits"
    storageType match {
      case EntryStorageType.BYTE => require(0 < fingerprintBits && fingerprintBits <= 8, storageTypeErrMsg)
      case EntryStorageType.SHORT => require(8 < fingerprintBits && fingerprintBits <= 16, storageTypeErrMsg)
      case EntryStorageType.INT => require(0 < fingerprintBits && fingerprintBits <= 8, storageTypeErrMsg)
      case EntryStorageType.LONG => require(0 < fingerprintBits && fingerprintBits <= 16, storageTypeErrMsg)
    }
    val fpReqErrMsg = "SimpleCuckooStrategy.create: 4 ^ (bucketSize * fingerprintBits) < \\Omega(capacity)"
    require(2 * bucketSize * fingerprintBits >= math.floor(math.log(capacity) / math.log(2)), fpReqErrMsg)
    new SimpleCuckooStrategy[E](capacity, numBuckets.get, bucketSize, maxIterations, fingerprintBits, storageType)(funnel)
  }

  def create[E](capacity: Int, bucketSize: Int, maxIterations: Int)(implicit funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    create(capacity, bucketSize, maxIterations, 8, EntryStorageType.BYTE)
  }

  def create[E](capacity: Int, bucketSize: Int, maxIterations: Int, storageType: EntryStorageType)(implicit funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    val fingerprintBits = storageType match {
      case EntryStorageType.BYTE => 8
      case EntryStorageType.SHORT => 16
      case EntryStorageType.INT => 8
      case EntryStorageType.LONG => 8
    }
    create(capacity, bucketSize, maxIterations, fingerprintBits, storageType)
  }

  def create[E](capacity: Int, bucketSize: Int, maxIterations: Int, fingerprintBits: Int)(implicit funnel: Funnel[_ >: E]): SimpleCuckooStrategy[E] = {
    val storageType = if (fingerprintBits <= 8) EntryStorageType.BYTE else EntryStorageType.SHORT
    create(capacity, bucketSize, maxIterations, fingerprintBits, storageType)
  }
}
