package probfilter.crdt.immutable

import org.scalatest.funsuite.AnyFunSuite
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.bloom.SimpleBloomStrategy

import scala.util.Random


final class GBloomFilterTest extends AnyFunSuite {
  private val strategy = SimpleBloomStrategy.create(1 << 10, 1e-2)

  test("GBloomFilter should contain added elements") {
    val seed = 42
    val load = 1 << 5
    val rnd = new Random(seed)
    val data = Seq.fill(load)(rnd.nextInt())
    val empty = new GBloomFilter(strategy)
    val filter = data.foldLeft(empty)((f, i) => f.add(i))
    data.foreach(i => assert(filter.contains(i)))
  }

  test("GBloomFilter should contain all added elements after merge") {
    val seed = 42
    val load = 1 << 5
    val rnd = new Random(seed)
    val data1 = Seq.fill(load)(rnd.nextInt())
    val data2 = Seq.fill(load)(rnd.nextInt())
    val empty = new GBloomFilter(strategy)
    val filter1 = data1.foldLeft(empty)((f, i) => f.add(i))
    val filter2 = data2.foldLeft(empty)((f, i) => f.add(i))
    val filter1m2 = filter1.merge(filter2)
    val filter2m1 = filter2.merge(filter1)
    data1.foreach(i => assert(filter1m2.contains(i)))
    data2.foreach(i => assert(filter1m2.contains(i)))
    data1.foreach(i => assert(filter2m1.contains(i)))
    data2.foreach(i => assert(filter2m1.contains(i)))
  }
}
