ThisBuild / scalaVersion := "2.13.12"

val akkaVersion = "2.9.0"
val guavaVersion = "32.1.3-jre"
val scalatestVersion = "3.2.17"


lazy val commonSettings = Seq(
  version := "0.1.0-alpha",
)

lazy val root = Project("probfilter", file(".")).aggregate(
  core,
  akka,
  sample
)

lazy val core = Project("probfilter-core", file("probfilter-core")).settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "com.google.guava" % "guava" % guavaVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test
  ),
  assemblyShadeRules ++= Seq(
    ShadeRule.rename("com.google.**" -> "probfilter.shaded.@0").inAll
  )
)

lazy val akka = Project("probfilter-akka", file("probfilter-akka")).dependsOn(core).settings(
  commonSettings,
  resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion % Provided,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
  )
)

lazy val sample = Project("probfilter-sample", file("probfilter-sample")).dependsOn(akka).settings(
  commonSettings,
  resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion
  )
)
