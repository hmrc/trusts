import sbt.*

object AppDependencies {

  private val mongoHmrcVersion = "2.3.0"
  private val playBootstrapVersion = "9.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % mongoHmrcVersion,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % playBootstrapVersion,
    "com.github.java-json-tools"    % "json-schema-validator"       % "2.2.14",
    "uk.gov.hmrc"                   %% "tax-year"                   % "5.0.0",
    "commons-codec"                 % "commons-codec"               % "20041127.091804",
    "org.typelevel"                 %% "cats-core"                  % "2.12.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-30"     % playBootstrapVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-test-play-30"    % mongoHmrcVersion,
    "org.scalatestplus"             %% "scalacheck-1-17"            % "3.2.18.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID]        = compile ++ test

}
