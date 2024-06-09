package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.hash.Funnel
import com.c6h5no2.probfilter.pdsa.cuckoo.{CuckooEntryType, SimpleCuckooStrategy}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks


final class ORCuckooFilterTest extends AnyFlatSpec with TableDrivenPropertyChecks with CvRFilterTestOps {
  private var mutable: Boolean = false
  private var entryType: CuckooEntryType = CuckooEntryType.values().apply(0)

  private val configs = Table.apply(
    ("mutable", "entryType"),
    (false, CuckooEntryType.VERSIONED_INT),
    (false, CuckooEntryType.VERSIONED_LONG),
    (true, CuckooEntryType.VERSIONED_INT),
    (true, CuckooEntryType.VERSIONED_LONG),
  )

  behavior of "ORCuckooFilter"

  it should "contain added elements" in {
    forall(testContainAddedElem())
  }

  it should "abort add when saturated" in {
    forall(testAbortAddWhenSaturated())
  }

  it should "contain added elements after merge" in {
    forall(testContainAddedElemAfterMerge())
  }

  it should "not contain removed elements" in {
    forall(testNotContainRemovedElem())
  }

  it should "not contain observed-removed elements" in {
    forall(testNotContainObservedRemovedElem())
  }

  it should "contain concurrently added elements" in {
    forall(testContainConcurrentlyAddedElem())
  }

  override def supplyFilter(capacity: Int, rid: Short, funnel: Funnel[Int]): FluentCvRFilter[Int] = {
    val strategy = SimpleCuckooStrategy.apply(capacity, 2, 20, entryType, funnel)
    ORCuckooFilter.apply(mutable, strategy, rid, rid).asFluent()
  }

  private def forall[U](testFn: => U): Unit = {
    forAll(configs) { (mutable, entryType) =>
      this.mutable = mutable
      this.entryType = entryType
      testFn
    }
  }
}
