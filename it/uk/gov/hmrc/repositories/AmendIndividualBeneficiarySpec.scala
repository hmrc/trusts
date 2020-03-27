package uk.gov.hmrc.repositories

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatest.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, route, running, status}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.GetTrustSuccessResponse
import uk.gov.hmrc.trusts.utils.JsonUtils
import play.api.test.Helpers._
import uk.gov.hmrc.trusts.models.variation.IndividualDetailsType

import scala.concurrent.Future

class AmendIndividualBeneficiarySpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest with ScalaFutures {

  val getTrustResponseFromDES: GetTrustSuccessResponse =
    JsonUtils.getJsonValueFromFile("trusts-etmp-received.json").as[GetTrustSuccessResponse]

  val expectedInitialGetJson: JsValue =
    JsonUtils.getJsonValueFromFile("trusts-integration-get-initial.json")

  "an amend individual beneficiary call" - {

    "must return amended data in a subsequent 'get' call" in {

      val expectedGetAfterAmendBeneficiaryJson: JsValue =
        JsonUtils.getJsonValueFromFile("trusts-integration-get-after-amend-individual-beneficiary.json")

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

          // initial get
          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(result) mustBe OK
          contentAsJson(result) mustBe expectedInitialGetJson

          val payload = Json.parse(
            """
              |{
              |  "lineNo": "1",
              |  "name": {
              |    "firstName": "John",
              |    "middleName": "William",
              |    "lastName": "Wilson"
              |  },
              |  "dateOfBirth": "2010-01-01",
              |  "vulnerableBeneficiary": false,
              |  "beneficiaryType": "Director",
              |  "beneficiaryDiscretion": true,
              |  "beneficiaryShareOfIncome": "100",
              |  "identification": {
              |    "nino": "JP121212A"
              |  },
              |  "entityStart": "1998-02-12"
              |}
              |""".stripMargin)

          // amend individual beneficiary
          val amendRequest = FakeRequest(POST, "/trusts/amend-individual-beneficiary/5174384721/0")
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
