package uk.gov.hmrc.itbase

import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.{Application, Play}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.api.{DefaultDB, MongoConnection}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import repositories.TrustsMongoDriver
import play.api.test.Helpers.stubControllerComponents
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

trait IntegrationTestBase extends ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(15, Millis))

  val connectionString = "mongodb://localhost:27017/trusts-integration"

  def getDatabase(connection: MongoConnection): DefaultDB = {
    Await.result(connection.database("trusts-integration"), Duration.Inf)
  }

  def getConnection(application: Application): Try[MongoConnection] = {
    val mongoDriver = application.injector.instanceOf[TrustsMongoDriver]
    for {
      uri <- MongoConnection.parseURI(connectionString)
      connection: MongoConnection <- mongoDriver.api.driver.connection(uri, strictUri = true)
    } yield connection
  }

  def dropTheDatabase(connection: MongoConnection): Unit = {
    Await.result(getDatabase(connection).drop(), Duration.Inf)
  }

  private val cc = stubControllerComponents()

  def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(Seq(
        "mongodb.uri" -> connectionString,
        "metrics.enabled" -> false,
        "auditing.enabled" -> false,
        "mongo-async-driver.akka.log-dead-letters" -> 0,
        "features.mongo.dropIndexes" -> true
      ): _*)

  def createApplication : Application = applicationBuilder
    .overrides(
      bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Agent))
    ).build()

  def assertMongoTest(application: Application)(block: Application => Assertion): Future[Assertion] = {

    Play.start(application)

    try {

      val f: Future[Assertion] = for {
          connection <- Future.fromTry(getConnection(application))
          _ = dropTheDatabase(connection)
        } yield {
          block(application)
        }

      // We need to force the assertion to resolve here.
      // Otherwise, the test block may never be run at all.
      val assertion = Await.result(f, Duration.Inf)
      Future.successful(assertion)
    }
    finally {
      Play.stop(application)
    }
  }
}
