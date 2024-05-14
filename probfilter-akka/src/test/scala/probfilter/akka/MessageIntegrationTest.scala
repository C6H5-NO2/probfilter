package probfilter.akka

import akka.actor.testkit.typed.scaladsl.{FishingOutcomes, ScalaTestWithActorTestKit}
import org.scalatest.funsuite.AnyFunSuiteLike
import probfilter.crdt.immutable.ORCuckooFilter
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.cuckoo.{CuckooStrategy, EntryStorageType, SimpleCuckooStrategy}

import scala.util.Random


final class MessageIntegrationTest extends ScalaTestWithActorTestKit(BaseIntegrationTests.Configs.basic) with AnyFunSuiteLike {
  test("Messages should be serialized by Akka") {
    val strategy = SimpleCuckooStrategy.create(1 << 5, 2, 20, EntryStorageType.VERSIONED_LONG)
    val filter = new ORCuckooFilter(strategy, 1)
    val replicator = new FilterReplicator(new ReplicatedFilterKey("42"), filter)
    val actor = spawn(replicator.create())
    val probe = createTestProbe[Messages.Response]()

    val seed = 42
    val load = strategy.capacity
    val rnd = new Random(seed)
    val data = Seq.fill(load)(rnd.nextInt())

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
