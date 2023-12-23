package probfilter.pdsa

import probfilter.hash.{Funnel, MurmurHash3}

import java.util.{Iterator => JavaIterator}
import scala.collection.AbstractIterator


@SerialVersionUID(1L)
class BloomStrategy[T](val numBits: Int, val numHashes: Int)
                      (implicit val funnel: Funnel[_ >: T]) extends Serializable {
  def iterator(elem: T): Iterator[Int] = {
    val hash = MurmurHash3.hash(elem)
    val hash1 = hash.toInt
    val hash2 = (hash >>> 32).toInt
    new AbstractIterator[Int] {
      private var combined = hash1
      private var i = 0

      override def hasNext: Boolean = i < numHashes

      override def next(): Int = {
        val result = (combined & Int.MaxValue) % numBits
        combined += hash2
        i += 1
        result
      }
    }
  }

  def iteratorAsJava(elem: T): JavaIterator[Integer] = new JavaIterator[Integer] {
    private val scalaIterator = iterator(elem)

    override def hasNext: Boolean = scalaIterator.hasNext

    override def next(): Integer = scalaIterator.next()
  }
}


object BloomStrategy {
  def create[T](fpp: Double, capacity: Int)(implicit funnel: Funnel[_ >: T]): BloomStrategy[T] = {
    require(fpp > 0 && fpp < 1, s"BloomStrategy.create: fpp = $fpp not in (0, 1)")
    require(capacity > 0, s"BloomStrategy.create: capacity = $fpp not > 0")
    val m = optimalBits(fpp, capacity)
    require(m > 0 && m < Int.MaxValue, s"BloomStrategy.create: optimalBits = $m too large")
    val h = optimalHashes(fpp)
    require(h > 0 && h < Byte.MaxValue, s"BloomStrategy.create: optimalHashes = $h too large")
    new BloomStrategy[T](m, h)
  }

  private val ln2: Double = math.log(2)

  private def optimalBits(p: Double, n: Int): Int = math.ceil(n * -math.log(p) / (ln2 * ln2)).toInt

  private def optimalHashes(p: Double): Int = math.ceil(-math.log(p) / ln2).toInt
}
