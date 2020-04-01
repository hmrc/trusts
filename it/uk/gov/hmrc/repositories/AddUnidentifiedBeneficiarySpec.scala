package uk.gov.hmrc.repositories

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustSuccessResponse
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class AddUnidentifiedBeneficiarySpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar with TransformIntegrationTest {

  lazy val getTrustResponseFromDES: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  lazy val expectedInitialGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "an add unidentified beneficiary call" - {

    "must return amended data in a subsequent 'get' call" in {

      val newBeneficiaryJson = Json.parse(
        """
          |{
          | "description": "New Beneficiary Description",
          | "entityStart": "2020-01-01"
          |}
          |""".stripMargin
      )

      lazy val expectedGetAfterAddBeneficiaryJson: JsValue =
        JsonUtils.getJsonValueFromFile("trusts-integration-get-after-add-unidentified-beneficiary.json")

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
          contentAsJson(result) mustEqual expectedInitialGetJson

          val addRequest = FakeRequest(POST, "/trusts/add-unidentified-beneficiary/5174384721")
            .withBody(newBeneficiaryJson)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val addResult = route(application, addRequest).get
          status(addResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustEqual expectedGetAfterAddBeneficiaryJson

          dropTheDatabase(connection)
        }.get
      }
    }
  }
}
