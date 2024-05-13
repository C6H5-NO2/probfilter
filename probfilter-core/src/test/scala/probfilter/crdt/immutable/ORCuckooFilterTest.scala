package probfilter.crdt.immutable

import org.scalatest.funsuite.AnyFunSuite
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.cuckoo.{CuckooStrategy, EntryStorageType, SimpleCuckooStrategy}

import scala.util.Random


final class ORCuckooFilterTest extends AnyFunSuite {
  private val strategy = SimpleCuckooStrategy.create(1 << 10, 2, 20, EntryStorageType.VERSIONED_LONG)

  test("ORCuckooFilter should contain added elements") {
    val seed = 42
    val load = 1 << 5
    val rnd = new Random(seed)
    val data = Seq.fill(load)(rnd.nextInt())
    val empty = new ORCuckooFilter(strategy, 1)
    val filter = data.foldLeft(empty)((f, i) => f.add(i))
    data.foreach(i => assert(filter.contains(i)))
  }

  test("ORCuckooFilter should abort add when saturated") {
    val seed = 42
    val load = 1 << 10
    val rnd = new Random(seed)
    val data = Seq.fill(load)(rnd.nextInt())
    val empty = new ORCuckooFilter(strategy, 1)
    assertThrows[CuckooStrategy.MaxIterationReachedException] {
      data.foldLeft(empty)((f, i) => f.add(i))
    }
  }

  test("ORCuckooFilter should contain all added elements after merge") {
    val seed = 42
    val load = 1 << 5
    val rnd = new Random(seed)
    val data1 = Seq.fill(load)(rnd.nextInt())
    val data2 = Seq.fill(load)(rnd.nextInt())
    val empty1 = new ORCuckooFilter(strategy, 1)
    val empty2 = new ORCuckooFilter(strategy, 2)
    val filter1 = data1.foldLeft(empty1)((f, i) => f.add(i))
    val filter2 = data2.foldLeft(empty2)((f, i) => f.add(i))
    val filter1m2 = filter1.merge(filter2)
    val filter2m1 = filter2.merge(filter1)
    data1.foreach(i => assert(filter1m2.contains(i)))
    data2.foreach(i => assert(filter1m2.contains(i)))
    data1.foreach(i => assert(filter2m1.contains(i)))
    data2.foreach(i => assert(filter2m1.contains(i)))
  }

  test("ORCuckooFilter should not contain removed elements") {
    val seed = 42
    val load = 1 << 5
    val rnd = new Random(seed)
    val data = Seq.fill(load)(rnd.nextInt())
    val empty = new ORCuckooFilter(strategy, 1)
    val added = data.foldLeft(empty)((f, i) => f.add(i))
    data.foreach(i => assert(added.contains(i)))
    val removed = data.foldLeft(added)((f, i) => f.remove(i))
    data.foreach(i => assert(!removed.contains(i)))
  }

  test("ORCuckooFilter should not contain observed-removed elements") {
    val seed = 42
    val rnd = new Random(seed)
    val data = rnd.nextInt()
    val empty1 = new ORCuckooFilter(strategy, 1)
    val empty2 = new ORCuckooFilter(strategy, 2)
    val filter1 = empty1.add(data)
    val filter2 = empty2
    val filter1m2 = filter1.merge(filter2)
    val filter2m1 = filter2.merge(filter1)
    val filter2m1r = filter2m1.remove(data)
    val filter1m2m2 = filter1m2.merge(filter2m1r)
    val filter2m1m1 = filter2m1r.merge(filter1m2)
    assert(!filter1m2m2.contains(data))
    assert(!filter2m1m1.contains(data))
  }

  test("ORCuckooFilter should contain concurrently added elements") {
    val seed = 42
    val rnd = new Random(seed)
    val data = rnd.nextInt()
    val empty1 = new ORCuckooFilter(strategy, 1)
    val empty2 = new ORCuckooFilter(strategy, 2)
    val filter1 = empty1.add(data)
    val filter2 = empty2
    val filter1m2 = filter1.merge(filter2)
    val filter2m1 = filter2.merge(filter1)
    val filter1m2a = filter1m2.add(data)
    val filter2m1r = filter2m1.remove(data)
    val filter1m2m2 = filter1m2a.merge(filter2m1r)
    val filter2m1m1 = filter2m1r.merge(filter1m2a)
    assert(filter1m2m2.contains(data))
    assert(filter2m1m1.contains(data))
  }
}
