import Dependencies._

ThisBuild / scalaVersion := "2.12.7"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "uk.gov.homeoffice"
ThisBuild / organizationName := "drt"

lazy val root = (project in file("."))
  .settings(
    name := "drt-api-import",
    resolvers += Resolver.bintrayRepo("mfglabs", "maven"),
    libraryDependencies ++= Def.setting(Seq(
      scalaTest % Test,
      logbackClassic,
      scalaLogging,
      typesafeConfig,
      sprayJson
    )).value,
    parallelExecution in Test := false
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
