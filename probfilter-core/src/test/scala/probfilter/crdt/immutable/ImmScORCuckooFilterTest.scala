package probfilter.crdt.immutable

import org.scalatest.exceptions.TestFailedException
import org.scalatest.funsuite.AnyFunSuite
import probfilter.crdt.{CvFilter, CvFilterTests}
import probfilter.hash.Funnel
import probfilter.pdsa.cuckoo.{CuckooStrategy, EntryStorageType, SimpleCuckooStrategy}


final class ImmScORCuckooFilterTest extends AnyFunSuite with CvFilterTests {
  override def supplyFilter(capacity: Int, rid: Short)(implicit funnel: Funnel[Int]): CvFilter[Int, _] = {
    val strategy = SimpleCuckooStrategy.create(capacity >> 2, 2, 20, 7, EntryStorageType.VERSIONED_LONG)
    new ScORCuckooFilter(strategy, rid)
  }

  test("ScORCuckooFilter should contain added elements") {
    testContainAddedElem()
  }

  test("ScORCuckooFilter should not abort add when possible") {
    val exception = intercept[TestFailedException] {testAbortAddWhenSaturated()}
    val name = classOf[CuckooStrategy.MaxIterationReachedException].getName
    val message = s"Expected exception $name to be thrown, but no exception was thrown"
    assert(exception.message.get == message)
  }

  test("ScORCuckooFilter should contain all added elements after merge") {
    testContainAddedElemAfterMerge()
  }

  test("ScORCuckooFilter should not contain removed elements") {
    testNotContainRemovedElem()
  }

  test("ScORCuckooFilter should not contain observed-removed elements") {
    testNotContainObservedRemovedElem()
  }

  test("ScORCuckooFilter should contain concurrently added elements") {
    testContainConcurrentlyAddedElem()
  }
}
