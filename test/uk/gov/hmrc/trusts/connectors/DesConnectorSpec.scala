/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.trusts.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.models.ExistingTrustResponse._
import uk.gov.hmrc.trusts.models.{ExistingTrustCheckRequest, ExistingTrustResponse}
import uk.gov.hmrc.trusts.utils.WireMockHelper

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DesConnectorSpec extends BaseConnectorSpec
  with GuiceOneAppPerSuite with WireMockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Seq("microservice.services.des.port" -> server.port(),
        "auditing.enabled" -> false): _*).build()


  lazy val connector: DesConnector = app.injector.instanceOf[DesConnector]

  lazy val request = ExistingTrustCheckRequest("trust name", postCode = Some("NE65TA"), "1234567890")


  ".checkExistingTrust" should {

    "return Matched " when {
      "trusts data match with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 200, """{"match": true}""")

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe Matched
      }
    }
    "return NotMatched " when {
      "trusts data does not with existing trusts." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 200, """{"match": false}""")

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe NotMatched
      }
    }

    "return BadRequest " when {
        "payload sent is not valid" in {
          val wrongPayloadRequest = request.copy(utr = "NOT A NUMBER STRING")
          val requestBody = Json.stringify(Json.toJson(wrongPayloadRequest))

          stubFor("/trusts/match", requestBody, 400, Json.stringify(jsonResponse400))

          val result = Await.result(connector.checkExistingTrust(wrongPayloadRequest), Duration.Inf)
          result mustBe BadRequest
        }
    }

    "return AlreadyRegistered " when {
      "trusts is already registered with provided details." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 409, Json.stringify(jsonResponseAlreadyRegistered))

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe AlreadyRegistered
      }
    }

    "return ServiceUnavailable " when {
      "des dependent service is not responding " in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 503, Json.stringify(jsonResponse503))

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe ServiceUnavailable
      }
    }

    "return ServerError " when {
      "des is experiencing some problem." in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 500, Json.stringify(jsonResponse500))

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe ServerError
      }
    }

    "return ServerError " when {
      "des is returning forbidden response" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubFor("/trusts/match", requestBody, 409, "{}")

        val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
        result mustBe ServerError
      }
    }


  }


  def stubFor(url: String, requestBody: String, returnStatus: Int, responseBody: String) =
    server.stubFor(post(urlEqualTo(url))
      .withHeader("content-Type", containing("application/json"))
      .withHeader("Environment", containing("dev"))
      .withRequestBody(equalTo(requestBody))
      .willReturn(
        aResponse()
          .withStatus(returnStatus)
          .withBody(responseBody)))




}//end