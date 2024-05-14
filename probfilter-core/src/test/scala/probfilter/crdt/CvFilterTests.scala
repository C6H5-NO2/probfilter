package probfilter.crdt

import org.scalatest.Assertions
import probfilter.hash.Funnel
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.Filter
import probfilter.pdsa.cuckoo.CuckooStrategy

import scala.util.Random


trait CvFilterTests {
  this: Assertions =>

  import probfilter.crdt.CvFilterTests._

  def supplyFilter(capacity: Int, rid: Short)(implicit funnel: Funnel[Int]): CvFilter[Int, _]

  def testContainAddedElem(): Unit = {
    val rnd = new Random(seed)
    val data = Seq.fill(load)(rnd.nextInt())
    val empty = supplyFilter(capacity, 1)
    data.foreach(i => assert(!empty.contains(i)))
    val filter = incl(empty)(data)
    data.foreach(i => assert(filter.contains(i)))
  }

  def testNotContainRemovedElem(): Unit = {
    val rnd = new Random(seed)
    val data = Seq.fill(load)(rnd.nextInt())
    val empty = supplyFilter(capacity, 1)
    val added = incl(empty)(data)
    data.foreach(i => assert(added.contains(i)))
    val removed = excl(added)(data)
    data.foreach(i => assert(!removed.contains(i)))
  }

  /** @note cuckoo filter only */
  def testAbortAddWhenSaturated(): Unit = {
    val rnd = new Random(seed)
    val data = Seq.fill(capacity)(rnd.nextInt())
    val empty = supplyFilter(capacity, 1)
    assertThrows[CuckooStrategy.MaxIterationReachedException] {
      incl(empty)(data)
    }
  }

  def testContainAddedElemAfterMerge(): Unit = {
    val rnd = new Random(seed)
    val data1 = Seq.fill(load)(rnd.nextInt())
    val data2 = Seq.fill(load)(rnd.nextInt())
    val empty1 = supplyFilter(capacity, 1)
    val empty2 = supplyFilter(capacity, 2)
    val filter1 = incl(empty1)(data1)
    val filter2 = incl(empty2)(data2)
    val (filter1m2, filter2m1) = merge(filter1, filter2)
    data1.foreach(i => assert(filter1m2.contains(i)))
    data2.foreach(i => assert(filter1m2.contains(i)))
    data1.foreach(i => assert(filter2m1.contains(i)))
    data2.foreach(i => assert(filter2m1.contains(i)))
  }

  def testNotContainObservedRemovedElem(): Unit = {
    val rnd = new Random(seed)
    val data = rnd.nextInt()
    val empty1 = supplyFilter(capacity, 1)
    val empty2 = supplyFilter(capacity, 2)
    val filter1 = incl1(empty1)(data)
    val filter2 = empty2
    val (filter1m2, filter2m1) = merge(filter1, filter2)
    assert(filter1m2.contains(data))
    assert(filter2m1.contains(data))
    val filter1m2o = filter1m2
    val filter2m1r = excl1(filter2m1)(data)
    assert(filter1m2o.contains(data))
    assert(!filter2m1r.contains(data))
    val (filter1m2m2, filter2m1m1) = merge(filter1m2o, filter2m1r)
    assert(!filter1m2m2.contains(data))
    assert(!filter2m1m1.contains(data))
  }

  def testContainConcurrentlyAddedElem(): Unit = {
    val rnd = new Random(seed)
    val data = rnd.nextInt()
    val empty1 = supplyFilter(capacity, 1)
    val empty2 = supplyFilter(capacity, 2)
    val filter1 = incl1(empty1)(data)
    val filter2 = empty2
    val (filter1m2, filter2m1) = merge(filter1, filter2)
    assert(filter1m2.contains(data))
    assert(filter2m1.contains(data))
    val filter1m2a = incl1(filter1m2)(data)
    val filter2m1r = excl1(filter2m1)(data)
    assert(filter1m2a.contains(data))
    assert(!filter2m1r.contains(data))
    val (filter1m2m2, filter2m1m1) = merge(filter1m2a, filter2m1r)
    assert(filter1m2m2.contains(data))
    assert(filter2m1m1.contains(data))
  }
}


object CvFilterTests {
  private val seed: Int = 42

  private val capacity = 1 << 10

  private val load = 1 << 5

  private def incl1(filter: CvFilter[Int, _])(data: Int): CvFilter[Int, _] = {
    filter.add(data).asInstanceOf[CvFilter[Int, _]]
  }

  private def incl(filter: CvFilter[Int, _])(data: Iterable[Int]): CvFilter[Int, _] = {
    data.foldLeft[Filter[Int]](filter)((f, i) => f.add(i)).asInstanceOf[CvFilter[Int, _]]
  }

  private def excl1(filter: CvFilter[Int, _])(data: Int): CvFilter[Int, _] = {
    filter.remove(data).asInstanceOf[CvFilter[Int, _]]
  }

  private def excl(filter: CvFilter[Int, _])(data: Iterable[Int]): CvFilter[Int, _] = {
    data.foldLeft[Filter[Int]](filter)((f, i) => f.remove(i)).asInstanceOf[CvFilter[Int, _]]
  }

  private def merge(filter1: CvFilter[Int, _], filter2: CvFilter[Int, _]): (CvFilter[Int, _], CvFilter[Int, _]) = {
    val filter1m2 = filter1.asInstanceOf[Convergent[CvFilter[Int, _]]].merge(filter2)
    val filter2m1 = filter2.asInstanceOf[Convergent[CvFilter[Int, _]]].merge(filter1)
    (filter1m2, filter2m1)
  }
}
