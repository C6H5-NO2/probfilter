package probfilter.crdt.immutable

import org.scalatest.funsuite.AnyFunSuite
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.CuckooStrategy


class GCuckooFilterTest extends AnyFunSuite {
  private val strategy = CuckooStrategy.create(1e4.toInt, 2, 20)

  test("GCuckooFilter should contain added elements") {
    BaseFilterTests.testAdd(new GCuckooFilter(strategy), strategy.capacity)(this)
  }

  test("GCuckooFilter should abort `add` when saturated") {
    CuckooFilterTests.testAddSaturated(s => new GCuckooFilter(s))(this)
  }

  test("GCuckooFilter should contain all elements after merged") {
    BaseFilterTests.testMerge(new GCuckooFilter(strategy), strategy.capacity, strategy.fpp)(this)
  }

  test("GCuckooFilter should abort `add` when overflowed") {
    CuckooFilterTests.testAddOverflowed(s => new GCuckooFilter(s))(this)
  }
}
