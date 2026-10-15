import sbt.*

object AppDependencies {

  private val mongoHmrcVersion     = "2.14.0"
  private val playBootstrapVersion = "10.8.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % mongoHmrcVersion,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % playBootstrapVersion,
    "com.networknt"      % "json-schema-validator"     % "2.0.8" exclude ("com.fasterxml.jackson.core", "jackson-databind"),
    "uk.gov.hmrc"       %% "tax-year"                  % "6.0.0",
    "commons-codec"      % "commons-codec"             % "1.23.0",
    "org.typelevel"     %% "cats-core"                 % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % playBootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % mongoHmrcVersion,
    "org.scalatestplus" %% "scalacheck-1-20"         % "3.2.20.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}
