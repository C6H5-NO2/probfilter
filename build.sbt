ThisBuild / scalaVersion := "2.13.14"

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

val akkaResolver = Seq(resolvers += ("Akka library repository" at "https://repo.akka.io/maven"))

val sharedSettings = Seq(
  version := "0.1.1-SNAPSHOT",
  versionScheme := Some("semver-spec"),
  organization := "com.c6h5no2",
  libraryDependencies ++= Seq(
    scalatestID % Test
  ),
  publish / skip := true,
  pomIncludeRepository := { _ => false },
)

lazy val root = Project("probfilter", file(".")).settings(
  sharedSettings
).aggregate(
  core,
  akka,
  eval,
  sample,
  verifx
)

lazy val core = Project("probfilter-core", file("probfilter-core")).enablePlugins(
).settings(
  sharedSettings,
  libraryDependencies ++= Seq(
    guavaID,
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
  // assembly / assemblyOption ~= {_.withIncludeScala(false)},
  publish / skip := false,
  publishMavenStyle := true,
)

lazy val akka = Project("probfilter-akka", file("probfilter-akka")).dependsOn(core).settings(
  sharedSettings,
  akkaResolver,
  libraryDependencies ++= Seq(
    akkaClusterID % Provided,
    akkaActorTestkitID % Test
  ),
  publish / skip := false,
  publishMavenStyle := true,
  // `scaladoc` has problem compiling the code. Use `javadoc` only.
  // Perhaps in the future we can have `javadoc` for Java and `scaladoc` for Scala with interlinking.
  Compile / doc / sources := (Compile / doc / sources).value.filter(_.name.endsWith(".java"))
)

lazy val eval = Project("probfilter-eval", file("probfilter-eval")).dependsOn(akka).settings(
  sharedSettings,
  akkaResolver,
  libraryDependencies ++= Seq(
    akkaActorID,
    akkaClusterID
  )
)

lazy val sample = Project("probfilter-sample", file("probfilter-sample")).dependsOn(akka).settings(
  sharedSettings,
  akkaResolver,
  libraryDependencies ++= Seq(
    akkaActorID,
    akkaClusterID
  )
)

lazy val verifx = Project("probfilter-verifx", file("probfilter-verifx")).settings(
  sharedSettings,
  libraryDependencies ++= Seq(
    verifxID
  ),
  Compile / unmanagedJars ++= Seq(
    file("probfilter-verifx/lib/com.microsoft.z3.jar")
  )
)
