autoScalaLibrary := false

/* Global configuration */
lazy val javaVersion = "1.11"
lazy val akkaVersion = "2.6.19"
lazy val akkaGroup = "com.typesafe.akka"

lazy val commonSettings = Seq(
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
)
lazy val commonLibraries = Seq(
  libraryDependencies ++= Seq(
    akkaGroup %% "akka-actor-typed" % akkaVersion,
    akkaGroup %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "junit" % "junit" % "4.13.2" % Test
  )
)

/* Projects & Subprojects */
lazy val assignment03 = (project in file(".")).settings(
  commonSettings,
  name := "assignment03",
  version := "0.1",
  scalaVersion := "3.1.1"
)
lazy val exercise01 = (project in file("ex-01")).settings(
  commonSettings, commonLibraries,
  name := "exercise01",
  autoScalaLibrary := false,
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.36",
    "org.slf4j" % "slf4j-jdk14" % "1.7.36",
  )
)
lazy val exercise02 = (project in file("ex-02")).settings(
  commonSettings, commonLibraries,
  name := "exercise02",
  scalaVersion := "3.1.1",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.11",
    "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  )
)

/* Tasks */
// TODO add tasks