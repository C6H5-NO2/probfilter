package com.c6h5no2.probfilter.akka

import akka.actor.testkit.typed.scaladsl.{FishingOutcomes, ScalaTestWithActorTestKit}
import com.c6h5no2.probfilter.crdt.ORCuckooFilter
import com.c6h5no2.probfilter.hash.Funnels.IntFunnel
import com.c6h5no2.probfilter.pdsa.cuckoo.{CuckooEntryType, CuckooStrategy, SimpleCuckooStrategy}
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.util.Random


final class MessageIntegrationTest
  extends ScalaTestWithActorTestKit(IntegrationTestOps.Configs.basic)
  with AnyFunSuiteLike {
  test("Messages should be serialized by Akka") {
    val strategy = SimpleCuckooStrategy.apply(1 << 10, 2, 100, CuckooEntryType.VERSIONED_LONG, IntFunnel)
    val filter = new ORCuckooFilter.Immutable(strategy, 1)
    val replicator = FilterReplicator.apply(new ReplicatedFilterKey("42"), ReplicatedFilter.apply(filter))
    val actor = spawn(replicator)
    val probe = createTestProbe[Messages.Response]()

    val rng = new Random(42)
    val load = strategy.capacity
    val data = Seq.fill(load)(rng.nextInt())

    actor tell Messages.Contains(data.head, probe.ref)
    probe expectMessage Messages.ContainsResponse(data.head, false)

    actor tell Messages.Add(data.head, probe.ref)
    probe expectMessage Messages.AddResponse(data.head, None)

    actor tell Messages.Contains(data.head, probe.ref)
    probe expectMessage Messages.ContainsResponse(data.head, true)

    actor tell Messages.Remove(data.head, probe.ref)
    probe expectMessage Messages.RemoveResponse(data.head)

    actor tell Messages.Contains(data.head, probe.ref)
    probe expectMessage Messages.ContainsResponse(data.head, false)

    data.foreach(elem => actor tell Messages.Add(elem, probe.ref))
    var errorCaught = false
    probe.fishForMessage(this.patience.timeout) {
      case Messages.AddResponse(_, None) => FishingOutcomes.continueAndIgnore
      case Messages.AddResponse(_, Some(_: CuckooStrategy.MaxIterationReachedException)) => errorCaught = true; FishingOutcomes.complete
      case msg => FishingOutcomes.fail(msg.toString)
    }
    assert(errorCaught)
  }
}
