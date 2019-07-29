import sbt._

object Dependencies {
  lazy val scalaTest = "3.0.5"
  lazy val logbackClassic = "1.2.3"
  lazy val scalaLogging = "3.9.2"
  lazy val typesafeConfig = "1.3.2"
  lazy val renjin = "0.9.2725"
  lazy val akkaHttp = "10.1.8"
  lazy val akka    = "2.6.0-M4"
  lazy val scalapbV = "0.9.0-M1"
  lazy val akkaPersistenceJdbc = "3.5.3-DRT-SNAPSHOT"
  lazy val hikaricp = "3.2.2"
  lazy val postgres = "42.2.2"
  lazy val jodaTime = "2.10.3"
  lazy val drtLib = "0.2.0"

  val libDeps = Seq(
    "uk.gov.homeoffice" %% "drt-lib" % drtLib,
    "org.scalatest" %% "scalatest" % scalaTest,
    "ch.qos.logback" % "logback-classic" % logbackClassic,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLogging,
    "com.typesafe" % "config" % typesafeConfig,
    "org.renjin" % "renjin-script-engine" % renjin,

    "com.typesafe.akka" %% "akka-http"            % akkaHttp,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttp,
    "com.typesafe.akka" %% "akka-http-xml"        % akkaHttp,
    "com.typesafe.akka" %% "akka-persistence"     % akka,
    "com.typesafe.akka" %% "akka-persistence-query" % akka,
    "com.typesafe.akka" %% "akka-stream"          % akka,

    "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttp % Test,
    "com.typesafe.akka" %% "akka-testkit"         % akka     % Test,
    "com.typesafe.akka" %% "akka-stream-testkit"  % akka     % Test,
    "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test,

    "com.thesamet.scalapb" %% "compilerplugin"    % scalapbV,
    "com.github.dnvriend" %% "akka-persistence-jdbc" % akkaPersistenceJdbc,
    "com.typesafe.slick" %% "slick-hikaricp" % hikaricp,
    "org.postgresql" % "postgresql" % postgres,
    "joda-time" % "joda-time" % jodaTime
  )
}
