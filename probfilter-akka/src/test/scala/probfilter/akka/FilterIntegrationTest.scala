package probfilter.akka

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.funsuite.AnyFunSuiteLike
import probfilter.crdt.BaseFilter
import probfilter.crdt.immutable.{AWCuckooFilter, GBloomFilter, GCuckooFilter}
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.{BloomStrategy, CuckooStrategy}

import scala.util.Random


class FilterIntegrationTest
  extends ScalaTestWithActorTestKit(BaseIntegrationTests.Configs.basic) with AnyFunSuiteLike {
  test("GBloomFilter should be replicated by Akka") {
    val strategy = BloomStrategy.create(1e4.toInt, 0.01)
    val filters = Vector.fill(3)(new GBloomFilter(strategy))
    testReplication(filters)
  }

  test("GCuckooFilter should be replicated by Akka") {
    val strategy = CuckooStrategy.create(1e4.toInt, 2, 20)
    val filters = Vector.fill(3)(new GCuckooFilter(strategy))
    testReplication(filters)
  }

  test("AWCuckooFilter should be replicated by Akka") {
    val strategy = CuckooStrategy.create(1e4.toInt, 2, 20)
    val filters = Vector.apply(
      new AWCuckooFilter(strategy, 1),
      new AWCuckooFilter(strategy, 2),
      new AWCuckooFilter(strategy, 3)
    )
    testReplication(filters)
  }

  //noinspection NameBooleanParameters
  private def testReplication(filters: Iterable[BaseFilter[_, _]]): Unit = {
    assume(filters.size == 3)
    BaseIntegrationTests.withClusterFrom(filters) { actors =>
      val Vector(actor1, actor2, actor3) = actors
      val probe = createTestProbe[Messages.Response]()

      val rnd = new Random()
      val es = Vector.fill(10)(rnd.nextInt())
      es.foreach { e =>
        actor1 tell Messages.Add(e, probe.ref)
        probe expectMessage Messages.AddResponse(e, None)
      }

      es.foreach { e =>
        actor1 tell Messages.Contains(e, probe.ref)
        probe expectMessage Messages.ContainsResponse(e, true)
      }

      eventually {
        es.foreach { e =>
          actor2 tell Messages.Contains(e, probe.ref)
          probe expectMessage Messages.ContainsResponse(e, true)
        }
      }

      eventually {
        es.foreach { e =>
          actor3 tell Messages.Contains(e, probe.ref)
          probe expectMessage Messages.ContainsResponse(e, true)
        }
      }
    }(this)
  }
}
