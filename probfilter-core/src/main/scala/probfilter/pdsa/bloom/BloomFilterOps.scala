package probfilter.pdsa.bloom

import scala.collection.BitSet


object BloomFilterOps {
  def size(data: BitSet, strategy: BloomStrategy[_]): Int = {
    val n = data.size
    val m = strategy.numBits()
    val k = strategy.numHashes()
    if (n < k)
      0
    else if (n < m)
      math.round(-m.toDouble / k * math.log1p(-n.toDouble / m)).toInt
    else
      math.round(m.toDouble / k).toInt
  }

  def contains[E](data: BitSet, strategy: BloomStrategy[E], elem: E): Boolean = {
    strategy.hashIterator(elem).forall(data.contains)
  }
}
