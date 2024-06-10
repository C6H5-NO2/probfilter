package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.hash.Funnels.IntFunnel
import com.c6h5no2.probfilter.pdsa.bloom.SimpleBloomStrategy
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks


final class GBloomFilterTest extends AnyFlatSpec with TableDrivenPropertyChecks with CvRFilterTestOps {
  behavior of "GBloomFilter"

  it should "contain added elements" in {
    forall(testContainAddedElem())
  }

  it should "contain added elements after merge" in {
    forall(testContainAddedElemAfterMerge())
  }

  private var mutable: Boolean = false

  private val configs = Table.apply(
    "mutable",
    false,
    true,
  )

  private def forall[U](testFn: => U): Unit = {
    forAll(configs) { mutable =>
      this.mutable = mutable
      testFn
    }
  }

  override def supplyFilter(capacity: Int, rid: Short): FluentCvRFilter[Int] = {
    val strategy = SimpleBloomStrategy.apply(capacity, 1e-2, IntFunnel)
    GBloomFilter.apply(mutable, strategy).asFluent
  }
}
