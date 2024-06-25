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

val akkaResolver = Seq(Keys.resolvers += ("Akka library repository" at "https://repo.akka.io/maven"))

lazy val sharedSettings = Seq(
  version ~= { v =>
    if (System.getenv("GITHUB_ACTIONS") == "true")
      v // handled by sbt-dynver
    else
      "0.1.0-SNAPSHOT"
  },
  versionScheme := Some("semver-spec"),
  publish / skip := true,
  libraryDependencies ++= Seq(
    scalatestID % Test
  ),
)

lazy val publishSettings = Seq(
  publish / skip := false,
  // credentials += ???, // handled by sbt-sonatype
  crossPaths := false,
  description := "A lib for conflict-free replicated probabilistic filters.",
  developers := List(Developer(
    "c6h5-no2",
    "Junbo Xiong",
    "c6h5-no2@outlook.com",
    url("https://github.com/C6H5-NO2")
  )),
  homepage := Some(url("https://github.com/C6H5-NO2/probfilter")),
  licenses := Seq("Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  organization := "com.c6h5no2",
  organizationHomepage := Some(url("https://github.com/C6H5-NO2")),
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true, // handled by sbt-ci-release
  // publishTo := sonatypePublishToBundle.value, // handled by sbt-ci-release
  // scmInfo := ???, // handled by sbt-ci-release
  sonatypeCredentialHost := "central.sonatype.com", // i.e. sonatypeCentralHost
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
  publishSettings
)

lazy val akka = Project("probfilter-akka", file("probfilter-akka")).dependsOn(core).settings(
  sharedSettings,
  akkaResolver,
  libraryDependencies ++= Seq(
    akkaClusterID % Provided,
    akkaActorTestkitID % Test
  ),
  publishSettings,
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
  ),
  Test / parallelExecution := false
)
