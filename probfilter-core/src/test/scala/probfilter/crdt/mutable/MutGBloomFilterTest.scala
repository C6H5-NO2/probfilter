package probfilter.crdt.mutable

import org.scalatest.funsuite.AnyFunSuite
import probfilter.crdt.{CvFilter, CvFilterTests}
import probfilter.hash.Funnel
import probfilter.pdsa.bloom.SimpleBloomStrategy


final class MutGBloomFilterTest extends AnyFunSuite with CvFilterTests {
  override def supplyFilter(capacity: Int, rid: Short)(implicit funnel: Funnel[Int]): CvFilter[Int, _] = {
    val strategy = SimpleBloomStrategy.create(capacity, 1e-2)
    new GBloomFilter(strategy)
  }

  test("GBloomFilter should contain added elements") {
    testContainAddedElem()
  }

  test("GBloomFilter should contain all added elements after merge") {
    testContainAddedElemAfterMerge()
  }
}
