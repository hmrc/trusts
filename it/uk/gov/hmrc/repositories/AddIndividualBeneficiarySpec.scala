package uk.gov.hmrc.repositories

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustSuccessResponse
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class AddIndividualBeneficiarySpec extends FreeSpec with MustMatchers with ScalaFutures with MockitoSugar with TransformIntegrationTest {

  val getTrustResponseFromDES: GetTrustSuccessResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "an add individual beneficiary call" - {
    "must return amended data in a subsequent 'get' call" in {

      val newBeneficiaryJson = Json.parse(
        """
          |{
          |  "name":{
          |    "firstName":"First",
          |    "lastName":"Last"
          |  },
          |  "dateOfBirth":"2000-01-01",
          |  "vulnerableBeneficiary":false,
          |  "identification": {
          |    "nino": "nino"
          |  },
          |  "entityStart":"1990-10-10"
          |}
          |""".stripMargin
      )

      val expectedGetAfterAddBeneficiaryJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-integration-get-after-add-individual-beneficiary.json")

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

          val addRequest = FakeRequest(POST, "/trusts/add-individual-beneficiary/5174384721")
            .withBody(newBeneficiaryJson)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val addResult = route(application, addRequest).get
          status(addResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(newResult) mustBe OK
          contentAsJson(newResult) mustBe expectedGetAfterAddBeneficiaryJson

          dropTheDatabase(connection)
        }.get
      }
    }
  }
}
