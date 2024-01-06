package probfilter.crdt.immutable

import org.scalatest.funsuite.AnyFunSuite
import probfilter.crdt.immutable.BaseFilterTests.BaseFilterOps
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.BloomStrategy
import probfilter.util.LazyString


class GBloomFilterTest extends AnyFunSuite {
  private val strategy = BloomStrategy.create(1e4.toInt, 0.01)

  test("GBloomFilter should contain added elements") {
    BaseFilterTests.testAdd(new GBloomFilter(strategy), strategy.capacity)(this)
  }

  test("GBloomFilter can contain false positives") {
    val strategy = BloomStrategy.create(1e4.toInt, 0.01)
    val evens = 0 until (2 * strategy.capacity) by 2
    val filter = new GBloomFilter(strategy).addAll(evens)
    val odds = 1 until 2e3.toInt by 2
    val someKnownFps = Vector.apply(315, 581, 591, 1305, 1365, 1615, 1815, 2043)
    for (e <- odds) {
      if (someKnownFps.contains(e))
        assert(filter.contains(e), LazyString.format("!contain false positive %s", e))
      else
        assert(!filter.contains(e), LazyString.format("contain false negative %s", e))
    }
  }

  test("GBloomFilter should contain all elements after merged") {
    BaseFilterTests.testMerge(new GBloomFilter(strategy), strategy.capacity, strategy.fpp)(this)
  }
}
