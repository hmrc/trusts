package uk.gov.hmrc.repositories

import org.joda.time.DateTime
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, DisplayTrustTrusteeIndividualType}
import uk.gov.hmrc.trusts.repositories.TransformationRepository
import uk.gov.hmrc.trusts.transformers.{AddTrusteeIndTransform, ComposedDeltaTransform, SetLeadTrusteeIndTransform}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class TransformRepositorySpec extends FreeSpec with MustMatchers with ScalaFutures with IntegrationPatience {
  private val connectionString = "mongodb://localhost:27017/transform-integration"

  "a transform repository" - {
    "must be able to store and retrieve a payload" in {

      dropTheDatabase()

      val application = appBuilder.build()

      running(application) {

        val repository = application.injector.instanceOf[TransformationRepository]

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

  val data = ComposedDeltaTransform(Seq(SetLeadTrusteeIndTransform(
    DisplayTrustLeadTrusteeIndType(
        "",
        None,
        NameType("New", Some("lead"), "Trustee"),
        DateTime.parse("2000-01-01"),
        "",
        None,
        DisplayTrustIdentificationType(None, None, None, None),
        "now"
      )),
    AddTrusteeIndTransform(DisplayTrustTrusteeIndividualType(
      "lineNo",
      Some("bpMatchStatus"),
      NameType("New", None, "Trustee"),
      Some(DateTime.parse("2000-01-01")),
      Some("phoneNumber"),
      Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
      "entityStart"
    ))
    )
  )

  lazy val connection = for {
    uri <- MongoConnection.parseURI(connectionString)
    y <- MongoDriver().connection(uri, true)
  } yield y

  def database: Future[DefaultDB] = {
    for {
      connection <- Future.fromTry(connection)
      database   <- connection.database("transform-integration")
    } yield database
  }

  def dropTheDatabase(): Unit = Await.result(database.flatMap(_.drop()), Duration.Inf)}
