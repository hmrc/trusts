package uk.gov.hmrc.repositories

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.utils.JsonUtils
import scala.concurrent.Future

class RemoveTrusteeSpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  trait JsonFixtures {

    val getTrustResponseFromDES : JsValue = JsonUtils
      .getJsonValueFromFile("trusts-etmp-received-multiple-trustees.json")
  }

  "a remove trustee call" - {

    "must return amended data in a subsequent 'get' call" in new JsonFixtures {

      val stubbedDesConnector = mock[DesConnector]

      when(stubbedDesConnector.getTrustInfo(any())(any())).thenReturn(Future.successful(getTrustResponseFromDES.as[GetTrustSuccessResponse]))

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

          val removeAtIndex = Json.parse(
            """
              |{
              |	"index": 0,
              |	"endDate": "2010-10-10"
              |}
              |""".stripMargin)

          val amendRequest = FakeRequest(PUT, "/trusts/5174384721/trustees/remove")
            .withBody(Json.toJson(removeAtIndex))
            .withHeaders(CONTENT_TYPE -> "application/json")

          val firstRemoveResult = route(application, amendRequest).get
          status(firstRemoveResult) mustBe OK

          val secondRemoveResult = route(application, amendRequest).get
          status(secondRemoveResult) mustBe OK

          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed/trustees")).get
          status(newResult) mustBe OK

          val trustees = (contentAsJson(newResult) \ "trustees").as[JsArray]
          trustees mustBe Json.parse(
            """
              |[
              |            {
              |              "trusteeOrg": {
              |                "name": "Trustee Org 2",
              |                "phoneNumber": "0121546546",
              |                "identification": {
              |                  "utr": "5465416546"
              |                },
              |                "entityStart": "1998-02-12",
              |                "provisional": true
              |              }
              |            }
              |]
              |""".stripMargin)

          dropTheDatabase(connection)
        }
      }
    }
  }

}
