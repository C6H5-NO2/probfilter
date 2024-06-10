package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.hash.Funnels.IntFunnel
import com.c6h5no2.probfilter.pdsa.cuckoo.{CuckooEntryType, SimpleCuckooStrategy}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks


final class ScGCuckooFilterTest extends AnyFlatSpec with TableDrivenPropertyChecks with CvRFilterTestOps {
  behavior of "ScGCuckooFilter"

  it should "contain added elements" in {
    forall(testContainAddedElem())
  }

  it should "not abort add when possible" in {
    forall(testNotAbortAdd())
  }

  it should "contain added elements after merge" in {
    forall(testContainAddedElemAfterMerge())
  }

  private var mutable: Boolean = false
  private var entryType: CuckooEntryType = CuckooEntryType.values().apply(0)

  private val configs = Table.apply(
    ("mutable", "entryType"),
    (false, CuckooEntryType.SIMPLE_BYTE),
    (false, CuckooEntryType.SIMPLE_SHORT),
    (true, CuckooEntryType.SIMPLE_BYTE),
    (true, CuckooEntryType.SIMPLE_SHORT),
  )

  private def forall[U](testFn: => U): Unit = {
    forAll(configs) { (mutable, entryType) =>
      this.mutable = mutable
      this.entryType = entryType
      testFn
    }
  }

  override def supplyFilter(capacity: Int, rid: Short): FluentCvRFilter[Int] = {
    val strategy = SimpleCuckooStrategy.apply(capacity >> 2, 2, 20, 7, entryType, IntFunnel)
    ScGCuckooFilter.apply(mutable, strategy, rid).asFluent
  }
}
