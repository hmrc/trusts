package uk.gov.hmrc.repositories

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.running
import reactivemongo.api.MongoConnection
import uk.gov.hmrc.trusts.repositories.{CacheRepository, TrustsMongoDriver}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class CacheRepositorySpec extends FreeSpec with MustMatchers with ScalaFutures with IntegrationPatience {
  private val connectionString = "mongodb://localhost:27017/trusts-integration"

  "a playback repository" - {
    "must be able to store and retrieve a payload" in {

      val application = appBuilder.build()

      running(application) {
        getConnection(application).map { connection =>

          dropTheDatabase(connection)

          val repository = application.injector.instanceOf[CacheRepository]

          val storedOk = repository.set("UTRUTRUTR", "InternalId", data)
          storedOk.futureValue mustBe true

          val retrieved = repository.get("UTRUTRUTR", "InternalId")
            .map(_.getOrElse(fail("The record was not found in the database")))

          retrieved.futureValue mustBe data

          dropTheDatabase(connection)
        }.get
      }
    }
  }

  private lazy val appBuilder =  new GuiceApplicationBuilder().configure(Seq(
    "mongodb.uri" -> connectionString,
    "metrics.enabled" -> false,
    "auditing.enabled" -> false
  ): _*)

  val data = Json.obj("testField" -> "testValue")

  private def getDatabase(connection: MongoConnection) = {
    connection.database("transform-integration")
  }

  private def getConnection(application: Application) = {
    val mongoDriver = application.injector.instanceOf[TrustsMongoDriver]
    lazy val connection = for {
      uri <- MongoConnection.parseURI(connectionString)
      connection <- mongoDriver.api.driver.connection(uri, true)
    } yield connection
    connection
  }

  def dropTheDatabase(connection: MongoConnection): Unit = {
    Await.result(getDatabase(connection).flatMap(_.drop()), Duration.Inf)
  }

}
