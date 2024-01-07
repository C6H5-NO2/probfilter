package probfilter.akka

import akka.actor.testkit.typed.scaladsl.{FishingOutcomes, ScalaTestWithActorTestKit}
import org.scalatest.funsuite.AnyFunSuiteLike
import probfilter.crdt.immutable.AWCuckooFilter
import probfilter.hash.Funnels.IntFunnel
import probfilter.pdsa.CuckooStrategy


class MessageIntegrationTest
  extends ScalaTestWithActorTestKit(BaseIntegrationTests.Configs.basic) with AnyFunSuiteLike {
  //noinspection NameBooleanParameters
  test("Message should be serialized and sent by Akka") {
    /** @see [[probfilter.crdt.immutable.CuckooFilterTests.collisionStrategy]] */
    val strategy = CuckooStrategy.create(16, 1, 20)
    val replicator = new FilterReplicator(new ReplicatedFilterKey("2024"), new AWCuckooFilter(strategy, 0))
    val system = spawn(replicator.create())
    val probe = createTestProbe[Messages.Response]()

    system tell Messages.Contains(6, probe.ref)
    probe expectMessage Messages.ContainsResponse(6, false)

    system tell Messages.Add(6, probe.ref)
    probe expectMessage Messages.AddResponse(6, None)

    system tell Messages.Add(23, probe.ref)
    probe expectMessage Messages.AddResponse(23, None)

    system tell Messages.Add(169, probe.ref)
    probe.fishForMessage(this.patience.timeout) {
      case Messages.AddResponse(169, Some(_: CuckooStrategy.MaxIterationReachedException)) =>
        FishingOutcomes.complete
      case msg =>
        FishingOutcomes.fail(msg.toString)
    }

    system tell Messages.Contains(169, probe.ref)
    probe expectMessage Messages.ContainsResponse(169, false)

    system tell Messages.Remove(23, probe.ref)
    probe expectMessage Messages.RemoveResponse(23)

    system tell Messages.Add(169, probe.ref)
    probe expectMessage Messages.AddResponse(169, None)

    system tell Messages.Contains(169, probe.ref)
    probe expectMessage Messages.ContainsResponse(169, true)
  }
}
