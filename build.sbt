import uk.gov.hmrc.DefaultBuildSettings.itSettings

ThisBuild / scalaVersion := "2.13.13"
ThisBuild / majorVersion := 0

val appName = "trusts"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalaVersion := "2.13.12",
    PlayKeys.playDefaultPort := 9782,
    libraryDependencies ++= AppDependencies(),
    Compile / unmanagedSourceDirectories += baseDirectory.value / "resources",
    scalacOptions ++= Seq("-feature", "-Wconf:src=routes/.*:s"),
    scoverageSettings
  )

//lazy val IntegrationTest = config("it") extend Test

//
//lazy val itSettings2 = itSettings() ++ Seq(
//  unmanagedSourceDirectories   := Seq(
//    baseDirectory.value / "it"
//  ),
//  parallelExecution            := false,
//  fork                         := true,
//  javaOptions                  ++= Seq(
//    "-Dlogger.resource=logback-test.xml",
//    "-Dconfig.resource=test.application.conf"
//  )
//)

lazy val it = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())


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
    "testOnly.*",
    "com.kenshoo.play.metrics*.*",
    ".*RichJsValue.*"
  )

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle IntegrationTest/scalastyle")
