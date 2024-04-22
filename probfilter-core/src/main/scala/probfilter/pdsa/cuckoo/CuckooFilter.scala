package probfilter.pdsa.cuckoo

import probfilter.pdsa.Filter
import probfilter.util.FalseRandom

import scala.annotation.tailrec


@SerialVersionUID(1L)
final class CuckooFilter[E] private[cuckoo](val data: CuckooTable, val strategy: CuckooStrategy[E]) extends Filter[E] {
  def this(strategy: CuckooStrategy[E]) = this(
    strategy.storageType() match {
      case EntryStorageType.BYTE => CuckooTable.empty[Byte]
      case EntryStorageType.SHORT => CuckooTable.empty[Short]
      case EntryStorageType.LONG => CuckooTable.empty[Long]
    },
    strategy
  )

  private val fingerprintExtractor = FingerprintExtractor.create(strategy)

  override def size(): Int = data.typed.size

  override def capacity(): Int = strategy.capacity()

  override def fpp(): Double = strategy.fpp()

  override def contains(elem: E): Boolean = {
    val triple = strategy.hashAll(elem)
    val entry = triple.fp
    strategy.storageType() match {
      case EntryStorageType.BYTE => contains[Byte](triple, entry.asInstanceOf[Byte])
      case EntryStorageType.SHORT => contains[Short](triple, entry)
      case EntryStorageType.LONG => contains[Long](triple, entry)
    }
  }

  private def contains[T](triple: CuckooStrategy.Triple, entry: T): Boolean = {
    val data = this.data.typed[T]
    data.contains(triple.i, entry) || data.contains(triple.j, entry)
  }

  override def add(elem: E): CuckooFilter[E] = {
    val triple = strategy.hashAll(elem)
    val entry = triple.fp
    strategy.storageType() match {
      case EntryStorageType.BYTE => add[Byte](triple, entry.asInstanceOf[Byte], elem)
      case EntryStorageType.SHORT => add[Short](triple, entry, elem)
      case EntryStorageType.LONG => add[Long](triple, entry, elem)
    }
  }

  def add[T](triple: CuckooStrategy.Triple, entry: T, elem: Any): CuckooFilter[E] = {
    val data = this.data.typed[T]
    val i1 = triple.i
    val i2 = triple.j
    val s1 = data.size(i1)
    val s2 = data.size(i2)
    val c = strategy.bucketSize()
    if (s1 > c)
      throw new CuckooStrategy.BucketOverflowException(elem, i1)
    else if (s2 > c)
      throw new CuckooStrategy.BucketOverflowException(elem, i2)
    else if (s1 < c)
      copy(data.add(i1, entry))
    else if (s2 < c)
      copy(data.add(i2, entry))
    else
      copy(displace(new CuckooStrategy.Pair(triple.i, triple.fp), entry, 0, data, elem))
  }

  @tailrec
  private def displace[T](pair: CuckooStrategy.Pair, entry: T, attempts: Int, data: TypedCuckooTable[T], elem: Any): TypedCuckooTable[T] = {
    if (attempts >= strategy.maxIterations())
      throw new CuckooStrategy.MaxIterationReachedException(elem, strategy.maxIterations())
    val i1 = pair.i
    val s1 = data.size(i1)
    val c = strategy.bucketSize()
    if (s1 > c)
      throw new CuckooStrategy.BucketOverflowException(elem, i1)
    else if (s1 < c)
      data.add(i1, entry)
    else {
      val (displacedEntry, newData) = data.replace(i1, entry)
      val fp = fingerprintExtractor.extract(displacedEntry)
      val i2 = strategy.altIndexOf(i1, fp)
      displace(new CuckooStrategy.Pair(i2, fp), displacedEntry, attempts + 1, newData, elem)
    }
  }

  override def remove(elem: E): CuckooFilter[E] = {
    val triple = strategy.hashAll(elem)
    strategy.storageType() match {
      case EntryStorageType.BYTE => remove[Byte](triple, elem)
      case EntryStorageType.SHORT => remove[Short](triple, elem)
      case EntryStorageType.LONG => remove[Long](triple, elem)
    }
  }

  private def remove[T](triple: CuckooStrategy.Triple, elem: Any): CuckooFilter[E] = {
    val data = this.data.typed[T]
    val a1 = data.get(triple.i).filter(fingerprintExtractor.extract(_) == triple.fp)
    val a2 = data.get(triple.j).filter(fingerprintExtractor.extract(_) == triple.fp)
    val len = a1.length + a2.length
    if (len == 0)
      return this
    val rand = FalseRandom.next(len)
    if (rand < a1.length) {
      val entry = a1.apply(rand)
      val newData = data.remove(triple.i, entry)
      copy(newData)
    } else {
      val entry = a2.apply(rand - a1.length)
      val newData = data.remove(triple.j, entry)
      copy(newData)
    }
  }

  def copy(data: CuckooTable): CuckooFilter[E] = new CuckooFilter[E](data, strategy)

  override def toString: String = data.toString
}
