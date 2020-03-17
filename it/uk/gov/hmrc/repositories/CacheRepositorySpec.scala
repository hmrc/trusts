package uk.gov.hmrc.repositories

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.trusts.repositories.CacheRepository
import scala.concurrent.ExecutionContext.Implicits.global

class CacheRepositorySpec extends FreeSpec with MustMatchers with TransformIntegrationTest {

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
    "auditing.enabled" -> false,
    "mongo-async-driver.akka.log-dead-letters" -> 0
  ): _*)

  val data = Json.obj("testField" -> "testValue")

}
