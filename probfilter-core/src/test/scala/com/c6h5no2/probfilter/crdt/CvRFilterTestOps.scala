package com.c6h5no2.probfilter.crdt

import com.c6h5no2.probfilter.pdsa.cuckoo.CuckooStrategy.MaxIterationReachedException
import org.scalatest.Assertions

import scala.util.Random


trait CvRFilterTestOps {
  this: Assertions =>

  import com.c6h5no2.probfilter.crdt.CvRFilterTestOps._

  def supplyFilter(capacity: Int, rid: Short): FluentCvRFilter[Int]

  def testContainAddedElem(): Unit = {
    val rng = new Random(seed)
    val data = Seq.fill(load)(rng.nextInt())
    val empty = supplyFilter(capacity, 1)
    data.foreach(i => assert(!empty.contains(i)))
    val added = empty.incl(data)
    data.foreach(i => assert(added.contains(i)))
  }

  def testNotContainRemovedElem(): Unit = {
    val rng = new Random(seed)
    val data = Seq.fill(load)(rng.nextInt())
    val empty = supplyFilter(capacity, 1)
    val added = empty.incl(data)
    data.foreach(i => assert(added.contains(i)))
    val removed = added.excl(data)
    data.foreach(i => assert(!removed.contains(i)))
  }

  /** @note cuckoo filter only */
  def testAbortAddWhenSaturated(): Unit = {
    val rng = new Random(seed)
    val data = Seq.fill(capacity)(rng.nextInt())
    val empty = supplyFilter(capacity, 1)
    assertThrows[MaxIterationReachedException] {
      empty.incl(data)
    }
  }

  def testNotAbortAdd(): Unit = {
    val rng = new Random(seed)
    val data = Seq.fill(capacity)(rng.nextInt())
    val empty = supplyFilter(capacity, 1)
    val added = empty.incl(data)
    data.foreach(i => assert(added.contains(i)))
  }

  def testContainAddedElemAfterMerge(): Unit = {
    testContainAddedElemAfterMergeOne()
    testContainAddedElemAfterMergeAll()
  }

  private def testContainAddedElemAfterMergeOne(): Unit = {
    val rng = new Random(seed)
    val data1 = rng.nextInt()
    val data2 = data1 + 1
    val empty1 = supplyFilter(capacity, 1)
    val empty2 = supplyFilter(capacity, 2)
    val filter1 = empty1.add(data1)
    val filter2 = empty2
    assert(filter1.contains(data1))
    assert(!filter2.contains(data1))
    val (filter1m2, filter2m1) = merge(filter1, filter2)
    assert(filter1m2.contains(data1))
    assert(filter2m1.contains(data1))
    assert(!filter1m2.contains(data2))
    assert(!filter2m1.contains(data2))
    val filter1m2o = filter1m2
    val filter2m1a = filter2m1.add(data2)
    assert(!filter1m2o.contains(data2))
    assert(filter2m1a.contains(data2))
    val (filter1m2m2, filter2m1m1) = merge(filter1m2o, filter2m1a)
    assert(filter1m2m2.contains(data1))
    assert(filter2m1m1.contains(data1))
    assert(filter1m2m2.contains(data2))
    assert(filter2m1m1.contains(data2))
  }

  private def testContainAddedElemAfterMergeAll(): Unit = {
    val rng = new Random(seed)
    val data1 = Seq.fill(load)(rng.nextInt())
    val data2 = Seq.fill(load)(rng.nextInt())
    val empty1 = supplyFilter(capacity, 1)
    val empty2 = supplyFilter(capacity, 2)
    val filter1 = empty1.incl(data1)
    val filter2 = empty2.incl(data2)
    val (filter1m2, filter2m1) = merge(filter1, filter2)
    data1.foreach(i => assert(filter1m2.contains(i)))
    data2.foreach(i => assert(filter1m2.contains(i)))
    data1.foreach(i => assert(filter2m1.contains(i)))
    data2.foreach(i => assert(filter2m1.contains(i)))
  }

  def testNotContainObservedRemovedElem(): Unit = {
    val rng = new Random(seed)
    val data = rng.nextInt()
    val empty1 = supplyFilter(capacity, 1)
    val empty2 = supplyFilter(capacity, 2)
    val filter1 = empty1.add(data)
    val filter2 = empty2
    val (filter1m2, filter2m1) = merge(filter1, filter2)
    assert(filter1m2.contains(data))
    assert(filter2m1.contains(data))
    val filter1m2o = filter1m2
    val filter2m1r = filter2m1.excl(data)
    assert(filter1m2o.contains(data))
    assert(!filter2m1r.contains(data))
    val (filter1m2m2, filter2m1m1) = merge(filter1m2o, filter2m1r)
    assert(!filter1m2m2.contains(data))
    assert(!filter2m1m1.contains(data))
  }

  def testContainConcurrentlyAddedElem(): Unit = {
    val rng = new Random(seed)
    val data = rng.nextInt()
    val empty1 = supplyFilter(capacity, 1)
    val empty2 = supplyFilter(capacity, 2)
    val filter1 = empty1.incl(data)
    val filter2 = empty2
    val (filter1m2, filter2m1) = merge(filter1, filter2)
    assert(filter1m2.contains(data))
    assert(filter2m1.contains(data))
    val filter1m2a = filter1m2.incl(data)
    val filter2m1r = filter2m1.excl(data)
    assert(filter1m2a.contains(data))
    assert(!filter2m1r.contains(data))
    val (filter1m2m2, filter2m1m1) = merge(filter1m2a, filter2m1r)
    assert(filter1m2m2.contains(data))
    assert(filter2m1m1.contains(data))
  }
}

object CvRFilterTestOps {
  private val seed: Int = 42
  private val capacity: Int = 1 << 10
  private val load: Int = 1 << 5

  private def merge(
    filter1: FluentCvRFilter[Int],
    filter2: FluentCvRFilter[Int],
  ): (FluentCvRFilter[Int], FluentCvRFilter[Int]) = {
    val filter1m2 = filter1.merge(filter2)
    val filter2m1 = filter2.merge(filter1)
    (filter1m2, filter2m1)
  }

  private implicit final class FilterSet(private val filter: FluentCvRFilter[Int]) extends AnyVal {
    def incl(elem: Int): FluentCvRFilter[Int] = filter.add(elem)

    def incl(elems: Iterable[Int]): FluentCvRFilter[Int] = elems.foldLeft(filter)((f, e) => f.add(e))

    def excl(elem: Int): FluentCvRFilter[Int] = filter.remove(elem)

    def excl(elems: Iterable[Int]): FluentCvRFilter[Int] = elems.foldLeft(filter)((f, e) => f.remove(e))
  }
}
