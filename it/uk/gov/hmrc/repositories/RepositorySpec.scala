package uk.gov.hmrc.repositories

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import uk.gov.hmrc.trusts.repositories.Repository
import org.scalatest.Assertions.fail
import scala.concurrent.Future
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global


class RepositorySpec extends FreeSpec with MustMatchers with ScalaFutures with IntegrationPatience {
  private val connectionString = "mongodb://localhost:27017/trusts-integration"

  "a playback repository" - {
    "must be able to store and retrieve a payload" in {

      dropTheDatabase()

      val application = appBuilder.build()

      running(application) {

        val repository = application.injector.instanceOf[Repository]
        repository.started.futureValue

        val storedOk = repository.set("UTRUTRUTR", "InternalId", data)
        storedOk.futureValue mustBe true

        val retrieved = repository.get("UTRUTRUTR", "InternalId")
          .map(_.getOrElse(fail("The record was not found in the database")))

         whenReady(retrieved)(_ mustBe data)
      }

      dropTheDatabase()
    }
  }

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
