package uk.gov.hmrc.repositories

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.running
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import uk.gov.hmrc.trusts.repositories.Repository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class RepositorySpec extends FreeSpec with MustMatchers with ScalaFutures with IntegrationPatience {
  private val connectionString = "mongodb://localhost:27017/trusts-integration"

  "a playback repository" - {
    "must be able to store and retrieve a payload" in {

      dropTheDatabase()

      val application = appBuilder.build()

      running(application) {

        val repository = application.injector.instanceOf[Repository]

        val storedOk = repository.set("UTRUTRUTR", "InternalId", data)
        storedOk.futureValue mustBe true

        val retrieved = repository.get("UTRUTRUTR", "InternalId")
          .map(_.getOrElse(fail("The record was not found in the database")))

         retrieved.futureValue mustBe data
      }

      dropTheDatabase()
    }
  }

  private lazy val appBuilder =  new GuiceApplicationBuilder().configure(Seq(
    "mongodb.uri" -> connectionString,
    "metrics.enabled" -> false,
    "auditing.enabled" -> false
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

  def dropTheDatabase(): Unit = Await.result(database.flatMap(_.drop()), Duration.Inf)}
