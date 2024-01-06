package probfilter.crdt.immutable

import org.scalatest.Assertions
import probfilter.crdt.BaseFilter
import probfilter.util.LazyString

import scala.util.Random


object BaseFilterTests {
  def testAdd[T <: BaseFilter[Int, T]](empty: BaseFilter[Int, T], capacity: Int)(assert: Assertions): Unit = {
    val rnd = new Random()
    val es = Vector.fill(capacity)(rnd.nextInt())
    val filter = empty.addAll(es)
    for (e <- es) {
      assert.assert(filter.contains(e), LazyString.format("f !contain %s of %s", e, es))
    }
  }

  def testRemove[T <: BaseFilter[Int, T]](empty: BaseFilter[Int, T], capacity: Int)(assert: Assertions): Unit = {
    val rnd = new Random()
    val e = rnd.nextInt()
    var filter = empty.add(e)
    assert.assert(filter.contains(e), LazyString.format("f !contain %s", e))
    filter = filter.remove(e)
    assert.assert(!filter.contains(e), LazyString.format("f contain %s", e))
    val es = Vector.fill(capacity)(rnd.nextInt())
    filter = empty.addAll(es).removeAll(rnd.shuffle(es))
    for (e <- es) {
      assert.assert(!filter.contains(e), LazyString.format("f contain %s", e))
    }
  }

  def testMerge[T <: BaseFilter[Int, T]](empty: BaseFilter[Int, T], capacity: Int, fpp: Double)
                                        (assert: Assertions): Unit = {
    testMerge(empty, empty, capacity, fpp)(assert)
  }

  def testMerge[T <: BaseFilter[Int, T]](
    empty1: BaseFilter[Int, T], empty2: BaseFilter[Int, T], capacity: Int, fpp: Double
  )(assert: Assertions): Unit = {
    val rnd = new Random()
    val es = Vector.fill(capacity)(rnd.nextInt())
    val (es1, es2) = es.partition(_ => rnd.nextBoolean())

    val filter1 = empty1.addAll(es1)
    val filter2 = empty2.addAll(es2)

    val fpp1 = es2.count(e => filter1.contains(e) && !es1.contains(e)) / es1.size.toDouble
    val fpp2 = es1.count(e => filter2.contains(e) && !es2.contains(e)) / es2.size.toDouble
    assert.assert(fpp1 < fpp, LazyString.format("f1 fpp too high %s", fpp1))
    assert.assert(fpp2 < fpp, LazyString.format("f2 fpp too high %s", fpp2))

    val filter1m2 = filter1.merge(filter2)
    val filter2m1 = filter2.merge(filter1)
    for (e <- es) {
      assert.assert(filter1m2.contains(e), LazyString.format("f1m2 !contain %s of %s U %s", e, es1, es2))
      assert.assert(filter2m1.contains(e), LazyString.format("f2m1 !contain %s of %s U %s", e, es2, es1))
    }
  }

  implicit final class BaseFilterOps[E, T <: BaseFilter[E, T]](private val filter: BaseFilter[E, T]) extends AnyVal {
    @inline def addAll(iterable: Iterable[E]): T = iterable.foldLeft(filter)((f, e) => f.add(e)).asInstanceOf[T]

    @inline def removeAll(iterable: Iterable[E]): T = iterable.foldLeft(filter)((f, e) => f.remove(e)).asInstanceOf[T]
  }
}
