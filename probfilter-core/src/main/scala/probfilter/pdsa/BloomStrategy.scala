package probfilter.pdsa

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
        combined += hash2 ^ (numHashes - i)
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
