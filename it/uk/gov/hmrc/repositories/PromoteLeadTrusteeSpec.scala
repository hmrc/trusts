package uk.gov.hmrc.repositories

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
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

class PromoteLeadTrusteeSpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "a promote lead trustee call" - {

    "must return amended data in a subsequent 'get' call" in {

      val newTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("John", Some("William"), "O'Connor"),
        dateOfBirth = new DateTime(1965, 2, 10, 0, 0),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationType(
          None,
          Some("ST123456"),
          None,
          Some(AddressType(
            "221B Baker Street",
            "Suite 16",
            Some("Newcastle upon Tyne"),
            None,
            Some("NE1 2LA"),
            "GB"
          ))),
          None
      )

      val expectedGetAfterPromoteTrusteeJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-after-promote-trustee.json")

      val stubbedDesConnector = mock[DesConnector]
      when(stubbedDesConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(getTrustResponseFromDES))

      val application = applicationBuilder
        .overrides(
          bind[IdentifierAction].toInstance(new FakeIdentifierAction(Organisation)),
          bind[DesConnector].toInstance(stubbedDesConnector)
        )
        .build()

      running(application) {
        getConnection(application).map { connection =>
          dropTheDatabase(connection)
          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(result) mustBe OK
          contentAsJson(result) mustBe expectedInitialGetJson

          val promoteRequest = FakeRequest(POST, "/trusts/promote-trustee/5174384721/0")
            .withBody(Json.toJson(newTrusteeIndInfo))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val promoteResult = route(application, promoteRequest).get
          status(promoteResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustBe expectedGetAfterPromoteTrusteeJson

          dropTheDatabase(connection)
        }.get
      }
    }
  }

}
