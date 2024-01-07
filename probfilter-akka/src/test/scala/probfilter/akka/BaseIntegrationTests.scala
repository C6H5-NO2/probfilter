package probfilter.akka

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.cluster.MemberStatus
import akka.cluster.typed.{Cluster, Join}
import akka.testkit.SocketUtil
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import probfilter.crdt.BaseFilter


object BaseIntegrationTests {
  object Configs {
    val basic: Config = ConfigFactory.parseString(
      """
        |akka.actor.provider = "cluster"
        |akka.actor.allow-java-serialization = "on"
        |akka.actor.warn-about-java-serializer-usage = "off"
        |akka.cluster.downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
        |akka.cluster.jmx.multi-mbeans-in-same-jvm = "on"
        |akka.remote.artery.canonical.hostname = "127.0.0.1"
        |akka.remote.artery.canonical.port = 0
      """.stripMargin
    )

    def fill(n: Int)(systemname: String): Vector[Config] = {
      val ports = SocketUtil.temporaryServerAddresses(n).map(_.getPort)
      from(ports)(systemname)
    }

    def from(ports: Iterable[Int])(systemname: String): Vector[Config] = {
      import scala.jdk.CollectionConverters.IterableHasAsJava
      val hostname = basic.getString("akka.remote.artery.canonical.hostname")
      val seeds = ports.view.map(port => s"akka://$systemname@$hostname:$port").toVector
      val config = basic.withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds.asJava))
      ports.view.map(
        port => config.withValue("akka.remote.artery.canonical.port", ConfigValueFactory.fromAnyRef(port))
      ).toVector
    }
  }

  def withSystems(systems: Iterable[ActorSystem[_]])(fn: => Unit): Unit = {
    try {fn} finally {systems.foreach(ActorTestKit.shutdown)}
  }

  def joinCluster(systems: Iterable[ActorSystem[_]])(kit: ScalaTestWithActorTestKit): Unit = {
    val clusters = systems.view.map(Cluster.apply).toVector
    clusters.foreach(c => c.manager tell Join.apply(clusters.head.selfMember.address))
    implicit val config: kit.PatienceConfig = kit.patience
    kit.eventually {
      val state = clusters.head.state.members.toVector.map(_.status)
      kit.assertResult(Vector.fill(clusters.size)(MemberStatus.Up))(state)
    }
  }

  def withClusterFrom(filters: Iterable[BaseFilter[_, _]])
                     (fn: Vector[ActorSystem[Messages.Message]] => Unit)
                     (kit: ScalaTestWithActorTestKit): Unit = {
    val name = "ClusterSystem"
    val key = new ReplicatedFilterKey("2024")
    val systems = (Configs.fill(filters.size)(name) lazyZip filters).map { (config, filter) =>
      val replicator = new FilterReplicator(key, filter)
      ActorSystem.apply(replicator.create(), name, config)
    }
    withSystems(systems) {
      joinCluster(systems)(kit)
      fn(systems)
    }
  }
}
