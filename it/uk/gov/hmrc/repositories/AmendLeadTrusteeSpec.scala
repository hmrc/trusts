package uk.gov.hmrc.repositories

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType, GetTrustSuccessResponse}
import uk.gov.hmrc.trusts.models.{AddressType, NameType}
import uk.gov.hmrc.trusts.utils.JsonUtils
import scala.concurrent.Future

class AmendLeadTrusteeSpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "an amend lead trustee call" - {
    "must return amended data in a subsequent 'get' call" in {

      val newTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = new DateTime(1965, 2, 10, 0, 0),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationType(
          None,
          Some("newNino"),
          None,
          Some(AddressType(
            "1344 Army Road",
            "Suite 111",
            Some("Telford"),
            Some("Shropshire"),
            Some("TF1 5DR"),
            "GB"
          ))),
          None
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
          "auditing.enabled" -> false,
          "mongo-async-driver.akka.log-dead-letters" -> 0
        ): _*)
        .build()

      running(application) {
        getConnection(application).map { connection =>
          dropTheDatabase(connection)
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

          dropTheDatabase(connection)
        }
      }
    }
  }
}
