package probfilter.crdt.mutable

import org.scalatest.funsuite.AnyFunSuite
import probfilter.crdt.{CvFilter, CvFilterTests}
import probfilter.hash.Funnel
import probfilter.pdsa.cuckoo.{EntryStorageType, SimpleCuckooStrategy}


final class MutORCuckooFilterTest extends AnyFunSuite with CvFilterTests {
  override def supplyFilter(capacity: Int, rid: Short)(implicit funnel: Funnel[Int]): CvFilter[Int, _] = {
    val strategy = SimpleCuckooStrategy.create(capacity, 2, 20, EntryStorageType.VERSIONED_LONG)
    new ORCuckooFilter(strategy, rid)
  }

  test("ORCuckooFilter should contain added elements") {
    testContainAddedElem()
  }

  test("ORCuckooFilter should abort add when saturated") {
    testAbortAddWhenSaturated()
  }

  test("ORCuckooFilter should contain all added elements after merge") {
    testContainAddedElemAfterMerge()
  }

  test("ORCuckooFilter should not contain removed elements") {
    testNotContainRemovedElem()
  }

  test("ORCuckooFilter should not contain observed-removed elements") {
    testNotContainObservedRemovedElem()
  }

  test("ORCuckooFilter should contain concurrently added elements") {
    testContainConcurrentlyAddedElem()
  }
}
