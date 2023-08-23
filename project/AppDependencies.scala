import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val mongoHmrcVersion = "1.3.0"
  private val playBootstrapVersion = "7.21.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-28"         % mongoHmrcVersion,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"  % playBootstrapVersion,
    "com.github.java-json-tools"    % "json-schema-validator"       % "2.2.14",
    "uk.gov.hmrc"                   %% "tax-year"                   % "3.3.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.15.2",
    "commons-codec"                 % "commons-codec"               % "1.16.0",
    "org.typelevel"                 %% "cats-core"                  % "2.10.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-28"     % playBootstrapVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-test-play-28"    % mongoHmrcVersion,
    "org.scalatest"                 %% "scalatest"                  % "3.2.16",
    "org.scalatestplus"             %% "scalacheck-1-17"            % "3.2.16.0",
    "org.scalatestplus"             %% "mockito-4-6"                % "3.2.15.0",
    "org.scalatestplus.play"        %% "scalatestplus-play"         % "5.1.0",
    "com.github.tomakehurst"        % "wiremock-standalone"         % "2.27.2",
    "com.vladsch.flexmark"          % "flexmark-all"                % "0.64.8",
    "com.typesafe.play"             %% "play-test"                  % PlayVersion.current
  ).map(_ % "test, it")

}
