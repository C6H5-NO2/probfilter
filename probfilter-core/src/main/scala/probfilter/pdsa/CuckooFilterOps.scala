package probfilter.pdsa

import scala.annotation.tailrec


object CuckooFilterOps {
  def add[E](triple: CuckooStrategy.Triple, entry: E, data: CuckooTable[E])
            (implicit strategy: CuckooStrategy[_], elem: Any): CuckooTable[E] = {
    val i1 = triple.i
    val i2 = triple.j
    val b1 = data.at(i1)
    val b2 = data.at(i2)
    val s1 = b1.size
    val s2 = b2.size
    val c = strategy.bucketSize
    if (s1 > c)
      throw new CuckooStrategy.BucketOverflowException(elem, i1)
    else if (s2 > c)
      throw new CuckooStrategy.BucketOverflowException(elem, i2)
    else if (s1 < c)
      b1.add(entry)
    else if (s2 < c)
      b2.add(entry)
    else {
      val extractor = entry match {
        case Byte => (e: E) => (e.asInstanceOf[Int] & 0xff).toShort
        case Short => (e: E) => e.asInstanceOf[Short]
        case Long => (e: E) => LongCuckooEntry.from(e.asInstanceOf[Long]).fingerprint
        case _ => (e: E) => e.asInstanceOf[Short]
      }
      displace(0, new CuckooStrategy.Pair(triple.fp, triple.i), entry, data)(strategy, elem, extractor)
    }
  }

  @tailrec
  private def displace[E](attempts: Int, pair: CuckooStrategy.Pair, entry: E, data: CuckooTable[E])
                         (implicit strategy: CuckooStrategy[_], elem: Any, extractor: E => Short): CuckooTable[E] = {
    if (attempts > strategy.maxIterations)
      throw new CuckooStrategy.MaxIterationReachedException(elem)

    val i1 = pair.i
    val b1 = data.at(i1)
    val s1 = b1.size
    val c = strategy.bucketSize
    if (s1 > c)
      throw new CuckooStrategy.BucketOverflowException(elem, i1)

    else if (s1 < c)
      b1.add(entry)

    else {
      val (displacedEntry, newData) = b1.replace(entry, rand(b1.size))
      val fp = extractor(entry)
      val i2 = strategy.getAltBucket(fp, i1)
      displace(attempts + 1, new CuckooStrategy.Pair(fp, i2), displacedEntry, newData)(strategy, elem, extractor)
    }
  }

  private var victimIdx: Int = -1

  def rand(until: Int): Int = {
    victimIdx = (victimIdx + 1) % until
    victimIdx
  }
}
