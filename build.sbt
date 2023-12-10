val akkaVersion = "2.9.0"
val guavaVersion = "32.1.3-jre"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.12",
  resolvers += "Akka library repository".at("https://repo.akka.io/maven")
)

lazy val root = (project in file(".")).aggregate(
  probfilter,
  probfilterSample
)

lazy val probfilter = (project in file("probfilter")).settings(
  commonSettings,
  name := "probfilter",
  version := "0.1.0-alpha",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
    "com.google.guava" % "guava" % guavaVersion
  )
)

lazy val probfilterSample = (project in file("probfilter-sample")).settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  )
).dependsOn(
  probfilter
)
