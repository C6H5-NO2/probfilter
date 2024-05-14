package probfilter.akka

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.funsuite.AnyFunSuiteLike
import probfilter.crdt.immutable.{GBloomFilter, GCuckooFilter, ImmCvFilter, ORCuckooFilter}
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.bloom.SimpleBloomStrategy
import probfilter.pdsa.cuckoo.{EntryStorageType, SimpleCuckooStrategy}

import scala.util.Random


final class FilterIntegrationTest extends ScalaTestWithActorTestKit(BaseIntegrationTests.Configs.basic) with AnyFunSuiteLike {
  private val seed = 42
  private val capacity = 1 << 10
  private val load = 1 << 4

  test("GBloomFilter should be replicated by Akka") {
    val strategy = SimpleBloomStrategy.create(capacity, 1e-2)
    val filters = Seq.fill(3)(new GBloomFilter(strategy))
    testReplication(filters)
  }

  test("GCuckooFilter should be replicated by Akka") {
    val strategy = SimpleCuckooStrategy.create(capacity, 2, 20)
    val filters = Seq.fill(3)(new GCuckooFilter(strategy))
    testReplication(filters)
  }

  test("ORCuckooFilter should be replicated by Akka") {
    val strategy = SimpleCuckooStrategy.create(capacity, 2, 20, EntryStorageType.VERSIONED_LONG)
    val filters = Range.inclusive(1, 3).map(i => new ORCuckooFilter(strategy, i.asInstanceOf[Short]))
    testReplication(filters)
  }

  private def testReplication(filters: Iterable[ImmCvFilter[_, _]]): Unit = {
    assume(filters.size == 3)
    BaseIntegrationTests.withClusterFrom(filters)(this) { actors =>
      val Seq(actor1, actor2, actor3) = actors
      val probe = createTestProbe[Messages.Response]()

      val rnd = new Random(seed)
      val data1 = Seq.fill(load)(rnd.nextInt())
      val data2 = Seq.fill(load)(rnd.nextInt())
      val data3 = Seq.fill(load)(rnd.nextInt())

      data1.foreach { elem =>
        actor1 tell Messages.Add(elem, probe.ref)
        probe expectMessage Messages.AddResponse(elem, None)
      }

      data1.foreach { elem =>
        actor1 tell Messages.Contains(elem, probe.ref)
        probe expectMessage Messages.ContainsResponse(elem, true)
      }

      data2.foreach { elem =>
        actor2 tell Messages.Add(elem, probe.ref)
        probe expectMessage Messages.AddResponse(elem, None)
      }

      data3.foreach { elem =>
        actor3 tell Messages.Add(elem, probe.ref)
        probe expectMessage Messages.AddResponse(elem, None)
      }

      val data = data1.concat(data2).concat(data3)

      eventually {
        data.foreach { elem =>
          actor1 tell Messages.Contains(elem, probe.ref)
          probe expectMessage Messages.ContainsResponse(elem, true)
        }
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
