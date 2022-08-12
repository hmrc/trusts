import sbt.Keys.baseDirectory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val IntegrationTest = config("it") extend(Test)

val appName = "trusts"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys

  val excludedPackages = Seq(
    "<empty>",
    ".*Reverse.*",
    ".*Routes.*",
    ".*standardError*.*",
    ".*main_template*.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    "config.*",
    "testOnlyDoNotUseInAppConf.*",
    "views.html.*",
    "testOnly.*",
    "com.kenshoo.play.metrics*.*",
    ".*LocalDateService.*",
    ".*LocalDateTimeService.*",
    ".*RichJsValue.*",
    ".*Repository.*"
  )

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalaVersion := "2.12.12",
    SilencerSettings(),
    majorVersion := 0,
    PlayKeys.playDefaultPort := 9782,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    dependencyOverrides ++= AppDependencies.overrides,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    publishingSettings,
    Compile / unmanagedSourceDirectories += baseDirectory.value / "resources"
  )
  .settings(scoverageSettings)
  .configs(IntegrationTest)
  .settings(inConfig(Test)(testSettings))
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(
    resolvers += Resolver.jcenterRepo
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork        := true,
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf",
    "-Dlogger.resource=logback-test.xml"
  )
)


lazy val itSettings = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories   := Seq(
    baseDirectory.value / "it"
  ),
  parallelExecution            := false,
  fork                         := true,
  javaOptions ++= Seq(
    "-Dlogger.resource=logback-test.xml"
  )
)