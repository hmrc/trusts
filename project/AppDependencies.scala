import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "org.reactivemongo"           %% "play2-reactivemongo"    % "0.18.3-play25",
    "uk.gov.hmrc"                 %% "bootstrap-play-25"      % "5.1.0",
    "com.github.java-json-tools"   % "json-schema-validator"  % "2.2.12"
  )

  val test: Seq[ModuleID] = Seq(
    "com.github.tomakehurst"   % "wiremock-standalone"  % "2.25.1",
    "org.mockito"              % "mockito-core"         % "3.2.4",
    "org.scalatestplus.play"  %% "scalatestplus-play"   % "2.0.1",
    "org.pegdown"              % "pegdown"              % "1.6.0",
    "org.scalatest"           %% "scalatest"            % "3.0.4",
    "uk.gov.hmrc"             %% "hmrctest"             % "3.9.0-play-25",
    "com.typesafe.play"       %% "play-test"            % PlayVersion.current
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
