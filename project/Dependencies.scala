import sbt._

object Dependencies {
  lazy val scalaTestV = "3.0.5"
  lazy val logbackClassicV = "1.2.3"
  lazy val scalaLoggingV = "3.9.2"
  lazy val typesafeConfigV = "1.3.2"
  lazy val renjinV = "0.9.2725"
  lazy val akkaHttpVersion = "10.1.8"
  lazy val akkaVersion    = "2.6.0-M4"

  val libDeps = Seq(
    "org.scalatest" %% "scalatest" % scalaTestV,
    "ch.qos.logback" % "logback-classic" % logbackClassicV,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingV,
    "com.typesafe" % "config" % typesafeConfigV,
    "org.renjin" % "renjin-script-engine" % renjinV,

    "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

    "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
    "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
    "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
  )
}
