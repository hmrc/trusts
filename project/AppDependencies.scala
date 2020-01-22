import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    ws,
    "org.reactivemongo" %% "play2-reactivemongo" % "0.18.3-play26",
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.11.0",
    "com.github.java-json-tools" % "json-schema-validator" % "2.2.8"
  )

  def test(scope: String = "test") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.8.0-play-25" % scope,
    "org.scalatest" %% "scalatest" % "3.0.4" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.github.tomakehurst" % "wiremock-standalone" % "2.17.0",
    "org.mockito" % "mockito-core" % "1.10.19",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope

  )

}
