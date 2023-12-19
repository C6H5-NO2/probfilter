package probfilter.pdsa

import probfilter.pdsa.CuckooStrategy.CuckooPair
import probfilter.pdsa.ScalaFunnels.IntFunnel


@SerialVersionUID(1L)
class CuckooStrategy[T](val numBuckets: Int, val bucketSize: Int, val maxIterations: Int)
                       (implicit val funnel: Funnel[_ >: T]) extends Serializable {
  def getCuckooPair(elem: T): CuckooPair = {
    val hash = MurmurHash3.hash(elem)
    val fp = (hash % 255 + 1).toByte
    new CuckooPair(fp, (hash & Int.MaxValue).toInt % numBuckets)
  }

  def getAltBucket(pair: CuckooPair): Int = getAltBucket(pair.fp, pair.i)

  def getAltBucket(fp: Byte, i: Int): Int = {
    (i ^ (MurmurHash3.hash(fp.toInt) & Int.MaxValue).toInt) % numBuckets
  }

  // def fpp: Double = {
  //   val p = 8
  //   bucketSize * 2.0 / (1 << p)
  // }
}


object CuckooStrategy {
  class CuckooPair(val fp: Byte, val i: Int)
}
