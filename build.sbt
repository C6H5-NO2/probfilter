ThisBuild / scalaVersion := "2.13.12"

val akkaVersion = "2.9.0"
val guavaVersion = "32.1.3-jre"

lazy val commonSettings = Seq(
  resolvers += "Akka library repository".at("https://repo.akka.io/maven")
)

lazy val root = Project("probfilter", file(".")).aggregate(
  core,
  sample
)

lazy val core = Project("probfilter-core", file("probfilter-core")).settings(
  commonSettings,
  version := "0.1.0-alpha",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
    "com.google.guava" % "guava" % guavaVersion,
    "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )
)

lazy val sample = Project("probfilter-sample", file("probfilter-sample")).dependsOn(core).settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  )
)
