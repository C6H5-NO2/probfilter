ThisBuild / scalaVersion := "2.13.12"

val akkaVersion = "2.9.0"
val guavaVersion = "32.1.3-jre"
val scalatestVersion = "3.2.17"

lazy val commonSettings = Seq(
  version := "0.1.0-alpha",
)

lazy val commonAkkaSettings = Seq(
  resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
)

lazy val root = Project("probfilter", file(".")).aggregate(
  core,
  verifx,
  akka,
  eval,
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
  ),
  // assemblyMergeStrategy := {
  //   case PathList("javax", _*) => MergeStrategy.discard
  //   case PathList("org", "checkerframework", _*) => MergeStrategy.discard
  //   case x =>
  //     val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
  //     oldStrategy(x)
  // },
  // assembly / assemblyOption ~= {_.withIncludeScala(false)}
)

lazy val verifx = Project("probfilter-verifx", file("probfilter-verifx")).settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "org.verifx" %% "verifx" % "1.0.2",
    "org.scalatest" %% "scalatest" % scalatestVersion % Test
  ),
  Compile / unmanagedJars ++= Seq(file("probfilter-verifx/lib/com.microsoft.z3.jar"))
)

lazy val akka = Project("probfilter-akka", file("probfilter-akka")).dependsOn(core).settings(
  commonSettings,
  commonAkkaSettings,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion % Provided,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
  )
)

lazy val eval = Project("probfilter-eval", file("probfilter-eval")).dependsOn(akka).settings(
  commonSettings,
  commonAkkaSettings,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion
  )
)

lazy val sample = Project("probfilter-sample", file("probfilter-sample")).dependsOn(akka).settings(
  commonSettings,
  commonAkkaSettings,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion
  )
)
