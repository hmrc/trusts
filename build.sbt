import TestPhases.oneForkedJvmPerTest
import sbt.Keys.baseDirectory
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.SbtArtifactory

lazy val IntegrationTest = config("it") extend(Test)

val appName = "trusts"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;trusts.Routes.*;estates.Routes.*;prod.*;testOnlyDoNotUseInAppConf.*;views.html.*;" +
      "uk.gov.hmrc.BuildInfo;app.*;prod.*;uk.gov.hmrc.trusts.config.*",
    ScoverageKeys.coverageMinimum := 97,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(majorVersion := 0)
  .settings(PlayKeys.playDefaultPort := 9782)
  .settings(
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .settings(
    publishingSettings ++ scoverageSettings: _*
  )
  .settings(unmanagedSourceDirectories in Compile += baseDirectory.value / "resources")
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(
    resolvers += Resolver.jcenterRepo
  )


lazy val itSettings = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories   := Seq(
    baseDirectory.value / "it"
  ),
  parallelExecution            := false,
  fork                         := true
)