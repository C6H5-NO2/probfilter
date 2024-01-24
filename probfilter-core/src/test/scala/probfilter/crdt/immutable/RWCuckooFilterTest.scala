package probfilter.crdt.immutable

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import probfilter.crdt.immutable.BaseFilterTests.BaseFilterOps
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.CuckooStrategy
import probfilter.util.LazyString

import scala.util.Random


class RWCuckooFilterTest extends AnyFunSuite with Matchers {
  private val strategy = CuckooStrategy.create(1e4.toInt, 2, 20)

  test("RWCuckooFilter should contain added elements") {
    BaseFilterTests.testAdd(new RWCuckooFilter(strategy, 0), strategy.capacity)(this)
  }

  test("RWCuckooFilter should not contain removed elements") {
    BaseFilterTests.testRemove(new RWCuckooFilter(strategy, 0), strategy.capacity)(this)
  }

  test("RWCuckooFilter should contain all elements after merged") {
    BaseFilterTests.testMerge(
      new RWCuckooFilter(strategy, 1), new RWCuckooFilter(strategy, 2), strategy.capacity, strategy.fpp
    )(this)
  }

  private def concurrentFixture = {
    val rnd = new Random()
    val es = Vector.fill(100)(rnd.nextInt())
    val (es1, es2) = es.partition(_ => rnd.nextBoolean())

    val filter1 = new RWCuckooFilter(strategy, 1).addAll(es1)
    val filter2 = new RWCuckooFilter(strategy, 2).addAll(es2)

    val e = es2.find(e => !filter1.contains(e))
    assert(e.isDefined, LazyString.format("f1 fpp too high created from %s tested with %s", es1, es2))

    (es1, es2, e.get, filter1, filter2)
  }

  test("RWCuckooFilter should let concurrent `remove` win against `add`") {
    var (es1, es2, e, filter1, filter2) = concurrentFixture

    filter1 = filter1.add(e)
    filter2 = filter2.remove(e)
    assert(!filter2.contains(e))

    val filter1m2 = filter1.merge(filter2)
    val filter2m1 = filter2.merge(filter1)
    assert(!filter1m2.contains(e), LazyString.format("f1m2 contains %s of %s U %s", e, es1, es2))
    assert(!filter2m1.contains(e), LazyString.format("f2m1 contains %s of %s U %s", e, es2, es1))
  }
}
