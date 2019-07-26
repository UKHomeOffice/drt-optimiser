import Dependencies._

ThisBuild / scalaVersion := "2.12.7"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "uk.gov.homeoffice"
ThisBuild / organizationName := "drt"

lazy val root = (project in file("."))
  .settings(
    name := "drt-optimiser",
    resolvers ++= Seq(
      "Mulesoft" at "https://repository.mulesoft.org/nexus/content/repositories/public/"
    ),
    libraryDependencies ++= libDeps,
    parallelExecution in Test := false,
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    )
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
