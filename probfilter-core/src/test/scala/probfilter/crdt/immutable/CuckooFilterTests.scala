package probfilter.crdt.immutable

import org.scalatest.Assertions
import probfilter.crdt.BaseFilter
import probfilter.crdt.immutable.BaseFilterTests.BaseFilterOps
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.CuckooStrategy


object CuckooFilterTests {
  private def collisionStrategy(assert: Assertions): CuckooStrategy[Int] = {
    val strategy = CuckooStrategy.create(16, 1, 20)
    val samples = Vector.apply(6 -> (98, 8, 8), 23 -> (243, 5, 5), 97 -> (164, 5, 5), 169 -> (49, 5, 8)).toMap
    //noinspection ComparingUnrelatedTypes
    samples.foreachEntry((e, t) => assert.assert(strategy.getCuckooTriple(e) equals t))
    strategy
  }

  def testAddSaturated[T <: BaseFilter[Int, T]](emptyFrom: CuckooStrategy[Int] => BaseFilter[Int, T])
                                               (assert: Assertions): Unit = {
    val strategy = collisionStrategy(assert)
    val es = Vector.apply(6, 23)
    val straw = 169
    val filter = emptyFrom(strategy).addAll(es)
    assert.assertThrows[CuckooStrategy.MaxIterationReachedException] {filter.add(straw)}
  }

  def testAddOverflowed[T <: BaseFilter[Int, T]](emptyFrom: CuckooStrategy[Int] => BaseFilter[Int, T])
                                                (assert: Assertions): Unit = {
    val strategy = collisionStrategy(assert)
    val es1 = Vector.apply(23)
    val es2 = Vector.apply(97)
    val straw = 169

    val filter1 = emptyFrom(strategy).addAll(es1)
    val filter2 = emptyFrom(strategy).addAll(es2)

    val filter1m2 = filter1.merge(filter2)
    assert.assertThrows[CuckooStrategy.BucketOverflowException] {filter1m2.add(straw)}
  }
}
