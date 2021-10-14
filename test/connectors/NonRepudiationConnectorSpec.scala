package connectors

import connector.NonRepudiationConnector
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import play.api.test.Helpers._
import org.scalatest.matchers.must.Matchers._
import utils.JsonUtils.jsonFromFile

class NonRepudiationConnectorSpec extends ConnectorSpecHelper {

  lazy val connector: NonRepudiationConnector = injector.instanceOf[NonRepudiationConnector]

  ".nonRepudiationConnector" should {

    val requestBody = jsonFromFile("/validRegistrationEvent.json")

    "return subscription Id  " when {

      "valid" in {

        val nonRepudiationEndpointUrl = s"/submission"
        stubForPost(server, nonRepudiationEndpointUrl, requestBody, ACCEPTED, """{"subscriptionId": "1234567890"}""")

        val futureResult = connector.NonRepudiat(Json.parse("""{"subscriptionId": "1234567890"}"""))

        whenReady(futureResult) {
          result =>
            status(result) mustBe ACCEPTED
            (contentAsJson(result) \ "subscriptionId").as[String] mustBe "1234567890"
        }
      }
    }
  }
}
