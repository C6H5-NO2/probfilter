ThisBuild / scalaVersion := "2.13.12"

val akkaVersion = "2.9.0"
val guavaVersion = "32.1.3-jre"

lazy val commonSettings = Seq(
  resolvers += "Akka library repository".at("https://repo.akka.io/maven")
)

lazy val root = Project("probfilter", file(".")).aggregate(
  core,
  akka,
  sample
)

lazy val core = Project("probfilter-core", file("probfilter-core")).settings(
  commonSettings,
  version := "0.1.0-alpha",
  libraryDependencies ++= Seq(
    "com.google.guava" % "guava" % guavaVersion,
  ),
  assemblyShadeRules ++= Seq(
    ShadeRule.rename("com.google.**" -> "probfilter.shaded.@0").inAll
  )
)

lazy val akka = Project("probfilter-akka", file("probfilter-akka")).dependsOn(core).settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  )
)

lazy val sample = Project("probfilter-sample", file("probfilter-sample")).dependsOn(akka).settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  )
)
