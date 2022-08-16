import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-28"         % "0.70.0",
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"  % "5.24.0",
    "com.github.java-json-tools"    % "json-schema-validator"       % "2.2.14",
    "uk.gov.hmrc"                   %% "tax-year"                   % "1.4.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.12.4"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-test-play-28"    % "0.70.0",
    "uk.gov.hmrc"                   %% "bootstrap-test-play-28"     % "5.24.0",
    "org.scalatest"                 %% "scalatest"                  % "3.2.9",
    "org.mockito"                   % "mockito-core"                % "3.11.2",
    "org.mockito"                   % "mockito-all"                 % "1.10.19",
    "org.pegdown"                   % "pegdown"                     % "1.6.0",
    "org.scalatestplus.play"        %% "scalatestplus-play"         % "5.1.0",
    "org.scalatestplus"             %% "scalacheck-1-15"            % "3.2.9.0",
    "org.scalatestplus"             %% "scalatestplus-mockito"      % "1.0.0-M2",
    "com.github.tomakehurst"        % "wiremock-standalone"         % "2.27.2",
    "com.vladsch.flexmark"          % "flexmark-all"                % "0.35.10",
    "com.typesafe.play"             %% "play-test"                  % PlayVersion.current
  ).map(_ % Test)

  val akkaVersion = "2.6.7"
  val akkaHttpVersion = "10.1.12"

  val overrides = Seq(
    "com.typesafe.akka" %% "akka-stream_2.12"     % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf_2.12"   % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j_2.12"      % akkaVersion,
    "com.typesafe.akka" %% "akka-actor_2.12"      % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core_2.12"  % akkaHttpVersion,
    "commons-codec"     % "commons-codec"         % "1.15"
  )

}
