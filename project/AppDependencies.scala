import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val testScope = "test"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "org.reactivemongo" %% "play2-reactivemongo" % "0.18.8-play26",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.14.0",
    "com.github.java-json-tools" % "json-schema-validator" % "2.2.8"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "hmrctest"            % "3.9.0-play-26" % testScope,
    "org.scalatest"          %% "scalatest"           % "3.0.8" % testScope,
    "org.pegdown"            % "pegdown"              % "1.6.0" % testScope,
    "com.github.tomakehurst" % "wiremock-standalone"  % "2.25.1",
    "org.mockito"            % "mockito-all"          % "1.10.19",
    "org.scalatestplus.play" %% "scalatestplus-play"  % "3.1.3",
    "com.typesafe.play"      %% "play-test"           % PlayVersion.current % testScope
  )

  val akkaVersion = "2.5.23"
  val akkaHttpVersion = "10.0.15"

  val overrides = Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  )

}
