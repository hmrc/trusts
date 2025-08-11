import uk.gov.hmrc.DefaultBuildSettings.itSettings

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / majorVersion := 0

val appName = "trusts"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    PlayKeys.playDefaultPort := 9782,
    libraryDependencies ++= AppDependencies(),
    Compile / unmanagedSourceDirectories += baseDirectory.value / "resources",
    scalacOptions ++= Seq("-feature", "-Wconf:src=routes/.*:s")
  )
  .settings(CodeCoverageSettings())

lazy val it = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings(true))
