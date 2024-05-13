package probfilter.akka

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.funsuite.AnyFunSuiteLike
import probfilter.crdt.immutable.{ImmCvFilter, GBloomFilter, GCuckooFilter, ORCuckooFilter}
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.bloom.SimpleBloomStrategy
import probfilter.pdsa.cuckoo.{EntryStorageType, SimpleCuckooStrategy}

import scala.util.Random


final class FilterIntegrationTest extends ScalaTestWithActorTestKit(BaseIntegrationTests.Configs.basic) with AnyFunSuiteLike {
  test("GBloomFilter should be replicated by Akka") {
    val strategy = SimpleBloomStrategy.create(1 << 10, 1e-2)
    val filters = Seq.fill(3)(new GBloomFilter(strategy))
    testReplication(filters)
  }

  test("GCuckooFilter should be replicated by Akka") {
    val strategy = SimpleCuckooStrategy.create(1 << 10, 2, 20)
    val filters = Seq.fill(3)(new GCuckooFilter(strategy))
    testReplication(filters)
  }

  test("ORCuckooFilter should be replicated by Akka") {
    val strategy = SimpleCuckooStrategy.create(1 << 10, 2, 20, EntryStorageType.VERSIONED_LONG)
    val filters = Range.inclusive(1, 3).map(i => new ORCuckooFilter(strategy, i.asInstanceOf[Short]))
    testReplication(filters)
  }

  private def testReplication(filters: Iterable[ImmCvFilter[_, _]]): Unit = {
    assume(filters.size == 3)
    BaseIntegrationTests.withClusterFrom(filters)(this) { actors =>
      val Seq(actor1, actor2, actor3) = actors
      val probe = createTestProbe[Messages.Response]()

      val seed = 42
      val load = 1 << 3
      val rnd = new Random(seed)
      val data = Seq.fill(load)(rnd.nextInt())

      data.foreach { elem =>
        actor1 tell Messages.Add(elem, probe.ref)
        probe expectMessage Messages.AddResponse(elem, None)
      }

      data.foreach { elem =>
        actor1 tell Messages.Contains(elem, probe.ref)
        probe expectMessage Messages.ContainsResponse(elem, true)
      }

      eventually {
        data.foreach { elem =>
          actor2 tell Messages.Contains(elem, probe.ref)
          probe expectMessage Messages.ContainsResponse(elem, true)
        }
      }

      eventually {
        data.foreach { elem =>
          actor3 tell Messages.Contains(elem, probe.ref)
          probe expectMessage Messages.ContainsResponse(elem, true)
        }
      }
    }
  }
}
