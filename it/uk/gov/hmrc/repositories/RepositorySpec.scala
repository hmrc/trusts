package uk.gov.hmrc.repositories

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import uk.gov.hmrc.trusts.repositories.Repository

import scala.concurrent.Future
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global


class RepositorySpec extends FreeSpec with MustMatchers with ScalaFutures with IntegrationPatience {
  private val connectionString = "mongodb://localhost:27017/trusts-integration"

  "a playback repository" - {
    "must be able to store a playback payload for a processed trust" in {

      dropTheDatabase()

      val application = appBuilder.build()

      running(application) {

        val repository = application.injector.instanceOf[Repository]
        repository.started.futureValue

        val storedOk = repository.set("UTRUTRUTR", "InternalId", data)

        storedOk.futureValue mustBe true
      }

      dropTheDatabase()
    }
  }

//  "must be able to refresh the session upon retrieval of user answers" in {
//
//    dropTheDatabase()
//
//    val application = appBuilder.build()
//
//    running(application) {
//
//      val repository = application.injector.instanceOf[Repository]
//      repository.started.futureValue
//
//      repository.set("UTRUTRUTR", "InternalId", data)
//
//      val firstGet = repository.get("UTRUTRUTR", "InternalId").map(_.get.updatedAt).futureValue
//
//      val secondGet = repository.get(userAnswers.internalAuthId).map(_.get.updatedAt).futureValue
//
//      secondGet isAfter firstGet mustBe true
//    }
//
//    dropTheDatabase()
//  }

  private lazy val appBuilder =  new GuiceApplicationBuilder().configure(Seq(
    "mongodb.uri" -> connectionString
  ): _*)

  val data = Json.obj("testField" -> "testValue")

  lazy val connection = for {
    uri <- MongoConnection.parseURI(connectionString)
    y <- MongoDriver().connection(uri, true)
  } yield y

  def database: Future[DefaultDB] = {
    for {
      connection <- Future.fromTry(connection)
      database   <- connection.database("trusts-integration")
    } yield database
  }

  def dropTheDatabase(): Future[Unit] = database.map(_.drop())
}
