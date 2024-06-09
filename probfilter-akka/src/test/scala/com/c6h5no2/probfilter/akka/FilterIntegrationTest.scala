package com.c6h5no2.probfilter.akka

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.c6h5no2.probfilter.crdt._
import com.c6h5no2.probfilter.hash.Funnels.IntFunnel
import com.c6h5no2.probfilter.pdsa.bloom.SimpleBloomStrategy
import com.c6h5no2.probfilter.pdsa.cuckoo.{CuckooEntryType, SimpleCuckooStrategy}
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.util.Random


final class FilterIntegrationTest
  extends ScalaTestWithActorTestKit(IntegrationTestOps.Configs.basic)
  with AnyFunSuiteLike
  with IntegrationTestOps {
  private val seed = 42
  private val capacity = 1 << 10
  private val load = 1 << 4

  test("GBloomFilter should be replicated by Akka") {
    val strategy = SimpleBloomStrategy.apply(capacity, 1e-2, IntFunnel)
    val filters = Seq.fill(3)(new GBloomFilter.Immutable(strategy))
    testReplication(filters.map(ReplicatedFilter.apply(_)))
  }

  test("GCuckooFilter should be replicated by Akka") {
    val strategy = SimpleCuckooStrategy.apply(capacity, 2, 100, IntFunnel)
    val filters = Seq.fill(3)(new GCuckooFilter.Immutable(strategy))
    testReplication(filters.map(ReplicatedFilter.apply(_)))
  }

  test("ORCuckooFilter should be replicated by Akka") {
    val strategy = SimpleCuckooStrategy.apply(capacity, 2, 100, CuckooEntryType.VERSIONED_LONG, IntFunnel)
    val filters = Range.inclusive(1, 3).map(rid => new ORCuckooFilter.Immutable(strategy, rid.toShort))
    testReplication(filters.map(ReplicatedFilter.apply(_)))
  }

  test("ScGBloomFilter should be replicated by Akka") {
    val strategy = SimpleBloomStrategy.apply(capacity, 1e-2, IntFunnel)
    val filters = Seq.fill(3)(new ScGBloomFilter.Immutable(strategy))
    testReplication(filters.map(ReplicatedFilter.apply(_)))
  }

  test("ScGCuckooFilter should be replicated by Akka") {
    val strategy = SimpleCuckooStrategy.apply(capacity, 2, 100, IntFunnel)
    val filters = Seq.fill(3)(new ScGCuckooFilter.Immutable(strategy, 0))
    testReplication(filters.map(ReplicatedFilter.apply(_)))
  }

  test("ScORCuckooFilter should be replicated by Akka") {
    val strategy = SimpleCuckooStrategy.apply(capacity, 2, 100, CuckooEntryType.VERSIONED_INT, IntFunnel)
    val filters = Range.inclusive(1, 3).map(rid => new ScORCuckooFilter.Immutable(strategy, rid.toShort, rid))
    testReplication(filters.map(ReplicatedFilter.apply(_)))
  }

  private def testReplication(filters: Seq[ReplicatedFilter]): Unit = {
    assume(filters.length == 3)
    withClusterFrom(filters) { actors =>
      val Seq(actor1, actor2, actor3) = actors
      val probe = createTestProbe[Messages.Response]()

      val rng = new Random(seed)
      val data1 = Seq.fill(load)(rng.nextInt())
      val data2 = Seq.fill(load)(rng.nextInt())
      val data3 = Seq.fill(load)(rng.nextInt())

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
      Thread.sleep(1500)

      data.foreach { elem =>
        eventually {
          actor1 tell Messages.Contains(elem, probe.ref)
          probe expectMessage Messages.ContainsResponse(elem, true)
        }
      }

      data.foreach { elem =>
        eventually {
          actor2 tell Messages.Contains(elem, probe.ref)
          probe expectMessage Messages.ContainsResponse(elem, true)
        }
      }

      data.foreach { elem =>
        eventually {
          actor3 tell Messages.Contains(elem, probe.ref)
          probe expectMessage Messages.ContainsResponse(elem, true)
        }
      }
    }
  }
}
