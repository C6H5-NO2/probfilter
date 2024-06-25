import sbt.*


object Dependencies {
  val akkaVersion = "2.9.3"
  val guavaVersion = "32.1.3-jre"
  val scalatestVersion = "3.2.18"
  val verifxVersion = "1.0.2"

  val akkaActorID = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaClusterID = "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion
  val akkaActorTestkitID = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
  val guavaID = "com.google.guava" % "guava" % guavaVersion
  val scalatestID = "org.scalatest" %% "scalatest" % scalatestVersion
  val verifxID = "org.verifx" %% "verifx" % verifxVersion

  val akkaResolver: Seq[Def.Setting[?]] =
    Seq(Keys.resolvers += ("Akka library repository" at "https://repo.akka.io/maven"))
}
