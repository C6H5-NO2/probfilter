package probfilter.crdt.mutable

import org.scalatest.funsuite.AnyFunSuite
import probfilter.crdt.{CvFilter, CvFilterTests}
import probfilter.hash.Funnel
import probfilter.pdsa.cuckoo.SimpleCuckooStrategy


final class MutGCuckooFilterTest extends AnyFunSuite with CvFilterTests {
  override def supplyFilter(capacity: Int, rid: Short)(implicit funnel: Funnel[Int]): CvFilter[Int, _] = {
    val strategy = SimpleCuckooStrategy.create(capacity, 2, 20)
    new GCuckooFilter(strategy)
  }

  test("GCuckooFilter should contain added elements") {
    testContainAddedElem()
  }

  test("GCuckooFilter should abort add when saturated") {
    testAbortAddWhenSaturated()
  }

  test("GCuckooFilter should contain all added elements after merge") {
    testContainAddedElemAfterMerge()
  }
}
