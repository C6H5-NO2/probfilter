package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.hash.Funnel
import com.c6h5no2.probfilter.pdsa.bloom.SimpleBloomStrategy
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks


final class ScGBloomFilterTest extends AnyFlatSpec with TableDrivenPropertyChecks with CvRFilterTestOps {
  private var mutable: Boolean = false

  private val configs = Table.apply(
    "mutable",
    false,
    true,
  )

  behavior of "ScGBloomFilter"

  it should "contain added elements" in {
    forall(testContainAddedElem())
  }

  it should "contain added elements after merge" in {
    forall(testContainAddedElemAfterMerge())
  }

  override def supplyFilter(capacity: Int, rid: Short, funnel: Funnel[Int]): FluentCvRFilter[Int] = {
    val strategy = SimpleBloomStrategy.apply(capacity >> 2, 1e-2, funnel)
    ScGBloomFilter.apply(mutable, strategy).asFluent()
  }

  private def forall[U](testFn: => U): Unit = {
    forAll(configs) { mutable =>
      this.mutable = mutable
      testFn
    }
  }
}
