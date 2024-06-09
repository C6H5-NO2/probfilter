package com.c6h5no2.probfilter.akka

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import akka.cluster.MemberStatus
import akka.cluster.typed.{Cluster, Join}
import akka.testkit.SocketUtil
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}


trait IntegrationTestOps {
  this: ScalaTestWithActorTestKit =>

  def withClusterFrom[U](filters: Seq[ReplicatedFilter])
                        (fn: Seq[ActorSystem[Messages.Message]] => U): U = {
    val name = "ClusterSystem"
    val key = new ReplicatedFilterKey("42")
    val configs = IntegrationTestOps.Configs.apply(name, filters.size)
    val systems = filters.lazyZip(configs).map { (filter, config) =>
      val replicator = FilterReplicator.apply(key, filter)
      ActorSystem.apply(replicator, name, config)
    }
    withSystems(systems) {
      joinCluster(systems)
      fn(systems)
    }
  }

  def withSystems[U](systems: Iterable[ActorSystem[_]])(fn: => U): U = {
    try {fn} finally {systems.foreach(ActorTestKit.shutdown)}
  }

  def joinCluster(systems: Iterable[ActorSystem[_]]): Unit = {
    val clusters = systems.view.map(Cluster.apply).toSeq
    clusters.foreach(c => c.manager tell Join.apply(clusters.head.selfMember.address))
    eventually {
      val status = clusters.head.state.members.toSeq.map(_.status)
      assertResult(Seq.fill(clusters.length)(MemberStatus.Up))(status)
    }
  }
}

object IntegrationTestOps {
  object Configs {
    /** The config to pass into the constructor of [[akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit]]. */
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

    /** @return a sequence of `n` configs bound on '127.0.0.1' with random available port */
    def apply(systemname: String, n: Int): Seq[Config] = {
      val hostname = basic.getString("akka.remote.artery.canonical.hostname")
      val ports = SocketUtil.temporaryServerAddresses(n, hostname).map(_.getPort)
      apply(systemname, hostname, ports)
    }

    def apply(systemname: String, ports: Iterable[Int]): Seq[Config] = {
      val hostname = basic.getString("akka.remote.artery.canonical.hostname")
      apply(systemname, hostname, ports)
    }

    def apply(systemname: String, hostname: String, ports: Iterable[Int]): Seq[Config] = {
      import scala.jdk.CollectionConverters.IterableHasAsJava
      val seeds = ports.map(port => s"akka://$systemname@$hostname:$port")
      val config = basic.withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds.asJava))
      ports.map(port => config.withValue("akka.remote.artery.canonical.port", ConfigValueFactory.fromAnyRef(port))).toSeq
    }
  }
}
