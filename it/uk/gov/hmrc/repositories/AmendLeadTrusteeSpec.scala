package uk.gov.hmrc.repositories

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, GetTrustSuccessResponse}
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class AmendLeadTrusteeSpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar {

  val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "an amend lead trustee call" - {
    "must return amended data in a subsequent 'get' call" in {

      dropTheDatabase()

      val newTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = "newLineNo",
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = new DateTime(1965, 2, 10, 0, 0),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
        entityStart = "2012-03-14"
      )

      val expectedGetAfterAmendLeadTrusteeJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-after-amend-lead-trustee.json")

      val stubbedDesConnector = mock[DesConnector]
      when(stubbedDesConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(getTrustResponseFromDES))

      val application = new GuiceApplicationBuilder()
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .configure(Seq(
          "mongodb.uri" -> connectionString,
          "metrics.enabled" -> false,
          "auditing.enabled" -> false
        ): _*)
        .build()

      running(application) {
        val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
        status(result) mustBe OK
        contentAsJson(result) mustBe expectedInitialGetJson

        val amendRequest = FakeRequest(POST, "/trusts/amend-lead-trustee/5174384721")
          .withBody(Json.toJson(newTrusteeIndInfo))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val amendResult = route(application, amendRequest).get
        status(amendResult) mustBe OK

        val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
        status(newResult) mustBe OK
        contentAsJson(newResult) mustBe expectedGetAfterAmendLeadTrusteeJson
      }

      dropTheDatabase()
    }
  }

  // We must be quite patient.
  implicit val defaultPatience = PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  // Database boilerplate
  private val connectionString = "mongodb://localhost:27017/trusts-integration"

  lazy val connection = for {
    uri <- MongoConnection.parseURI(connectionString)
    connection <- MongoDriver().connection(uri, true)
  } yield connection

  def database: Future[DefaultDB] = {
    for {
      connection <- Future.fromTry(connection)
      database   <- connection.database("trusts-integration")
    } yield database
  }

  def dropTheDatabase(): Unit = Await.result(database.flatMap(_.drop()), Duration.Inf)
}
