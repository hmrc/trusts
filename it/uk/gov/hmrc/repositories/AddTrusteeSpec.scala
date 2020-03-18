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

class AddTrusteeSpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  trait JsonFixtures {

    val getTrustResponseFromDES : JsValue = JsonUtils
      .getJsonValueFromFile("trusts-etmp-received-no-trustees.json")
  }

  "an add trustee call" - {

    "must return amended data in a subsequent 'get' call with provisional flags" in new JsonFixtures {

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

          // Ensure passes schema
          val result = route(application, FakeRequest(GET, "/trusts/5174384721/transformed")).get
          status(result) mustBe OK

          val addTrusteeJson = Json.parse(
            """
              |{
              |	"name": {
              |   "firstName": "Adam",
              |   "lastName": "Last"
              | },
              | "entityStart": "2020-03-03"
              |}
              |""".stripMargin)

          val amendRequest = FakeRequest(POST, "/trusts/add-trustee/5174384721")
            .withBody(addTrusteeJson)
            .withHeaders(CONTENT_TYPE -> "application/json")

          val addedResponse = route(application, amendRequest).get
          status(addedResponse) mustBe OK

          // ensure they're in the trust response with the provisional flag
          val newResult = route(application, FakeRequest(GET, "/trusts/5174384721/transformed/trustees")).get
          status(newResult) mustBe OK

          val trustees = (contentAsJson(newResult) \ "trustees").as[JsArray]
          trustees mustBe Json.parse(
            """
              |[
              |            {
              |              "trusteeInd": {
              |                "name": {
              |                 "firstName": "Adam",
              |                 "lastName": "Last"
              |                },
              |                "entityStart": "2020-03-03",
              |                "provisional": true
              |              }
              |            }
              |]
              |""".stripMargin)

          dropTheDatabase(connection)
        }.get
      }
    }

  }
}
