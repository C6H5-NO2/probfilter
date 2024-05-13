package probfilter.crdt.mutable

import probfilter.pdsa.cuckoo.mutable.CuckooFilter
import probfilter.pdsa.cuckoo.{CuckooStrategy, EntryStorageType, FingerprintExtractor}

import scala.reflect.ClassTag
import scala.util.Try


/** A mutable grow-only replicated cuckoo filter. */
@SerialVersionUID(1L)
final class GCuckooFilter[E] private(private val state: CuckooFilter[E]) extends MutCvFilter[E, GCuckooFilter[E]] {
  def this(strategy: CuckooStrategy[E]) = this(new CuckooFilter[E](strategy))

  {
    val storageType = state.strategy.storageType()
    require(
      storageType.ordinal() <= EntryStorageType.SIMPLE_SHORT.ordinal(),
      s"GCuckooFilter.<init>: storage type $storageType is not SIMPLE_*"
    )
  }

  def strategy: CuckooStrategy[E] = state.strategy

  override def size(): Int = state.size()

  override def capacity(): Int = state.capacity()

  override def fpp(): Double = state.fpp()

  override def contains(elem: E): Boolean = state.contains(elem)

  override def add(elem: E): GCuckooFilter[E] = {if (!contains(elem)) state.add(elem); this}

  override def tryAdd(elem: E): Try[GCuckooFilter[E]] = Try.apply(add(elem))

  override def merge(that: GCuckooFilter[E]): GCuckooFilter[E] = {
    this.strategy.storageType() match {
      case EntryStorageType.SIMPLE_BYTE => mergeImpl[Byte](that, FingerprintExtractor.create(state.strategy))
      case EntryStorageType.SIMPLE_SHORT => mergeImpl[Short](that, FingerprintExtractor.create(state.strategy))
    }
    this
  }

  private def mergeImpl[T: ClassTag](that: GCuckooFilter[E], extractor: FingerprintExtractor): Unit = {
    val thisData = this.state.data.typed[T]
    this.state.zipFold(that.state)(thisData) { (newData, thisBucket, thatBucket, index) =>
      thatBucket.foldLeft(newData) { (newData, entry) =>
        if (thisBucket.contains(entry)) {
          newData
        } else {
          val altIndex = this.state.strategy.altIndexOf(index, extractor.extract(entry))
          if (thisData.contains(altIndex, entry))
            newData
          else
            newData.add(altIndex, entry)
        }
      }
    }
  }

  override def toString: String = s"GCF($state)"
}
