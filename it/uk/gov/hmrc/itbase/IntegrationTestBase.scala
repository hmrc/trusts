package uk.gov.hmrc.itbase

import config.AppConfig
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import models.registration.RegistrationSubmissionDraft
import org.scalatest.{Assertion, BeforeAndAfter}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.test.Helpers.baseApplicationBuilder.injector
import play.api.test.Helpers.stubControllerComponents
import play.api.{Application, Play}
import repositories.{RegistrationSubmissionRepository, RegistrationSubmissionRepositoryImpl}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait IntegrationTestBase extends ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(15, Millis))

  val connectionString = "mongodb://localhost:27017/trusts-integration"

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

//  private def getDatabase(connection: MongoConnection): DefaultDB = {
//    Await.result(connection.database("trusts-integration"), Duration.Inf)
//  }
//
//  import reactivemongo.api._
//
//  private def getConnection(): Future[MongoConnection] = {
//    for {
//      uri <- Future.fromTry(MongoConnection.parseURI(connectionString))
//      connection <- AsyncDriver().connect(uri)
//    } yield connection
//  }
//
//  private def dropTheDatabase(connection: MongoConnection): Unit = {
//    Await.result(getDatabase(connection).drop(), Duration.Inf)
//  }


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

//  def assertMongoTest(application: Application)(block: Application => Assertion): Future[Assertion] = {
//
//    Play.start(application)
//
//    try {
//      val f: Future[Assertion] = for {
//          connection <- getConnection()
//          _ = dropTheDatabase(connection)
//          _ = connection.askClose()(Duration(1, "s"))
//        } yield {
//          block(application)
//        }
//
//      // We need to force the assertion to resolve here.
//      // Otherwise, the test block may never be run at all.
//      val assertion = Await.result(f, Duration.Inf)
//      Future.successful(assertion)
//    }
//    finally {
//      Play.stop(application)
//    }
//  }
}
