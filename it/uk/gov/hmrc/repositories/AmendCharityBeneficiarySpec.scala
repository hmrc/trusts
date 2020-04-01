package uk.gov.hmrc.repositories

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, route, running, status, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustSuccessResponse
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class AmendCharityBeneficiarySpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest with ScalaFutures {

  val getTrustResponseFromDES: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  val expectedInitialGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "an amend charity beneficiary call" - {

    "must return amended data in a subsequent 'get' call" in {

      val expectedGetAfterAmendBeneficiaryJson: JsValue =
        JsonUtils.getJsonValueFromFile("trusts-integration-get-after-amend-charity-beneficiary.json")

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

          val payload = Json.parse(
            """
              |{
              |  "lineNo": "1",
              |  "organisationName": "New Charity Name",
              |  "identification": {
              |    "utr": "1234567890"
              |  },
              |  "entityStart": "1998-02-12"
              |}
              |""".stripMargin)

          val amendRequest = FakeRequest(POST, "/trusts/amend-charity-beneficiary/5174384721/0")
            .withBody(payload)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val amendResult = route(application, amendRequest).get
          status(amendResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustEqual expectedGetAfterAmendBeneficiaryJson

          dropTheDatabase(connection)
        }
      }

    }

  }

}
