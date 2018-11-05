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
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.connector.DesConnector
import uk.gov.hmrc.trusts.models.{ExistingTrustCheckRequest, ExistingTrustResponse}
import uk.gov.hmrc.trusts.utils.WireMockHelper

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DesConnectorSpec extends BaseSpec
  with GuiceOneAppPerSuite with WireMockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Seq("microservice.services.des.port" -> server.port(),
        "auditing.enabled" -> false):_*).build()


  lazy val connector: DesConnector = app.injector.instanceOf[DesConnector]

  lazy val request = ExistingTrustCheckRequest("trust name", postCode = Some("123456"), "1234567890")


  ".checkExistingTrust" should {

    "return response" when {
      "response status is OK" when {
        "trusts data match with existing trusts." in {
          val requestBody = Json.stringify(Json.toJson(request))

          stubFor("/trusts/match",requestBody, 200, """{"match": true}""")

          val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
          result mustBe ExistingTrustResponse.Success(true)
        }

        "trusts data does not with existing trusts." in {
          val requestBody = Json.stringify(Json.toJson(request))

          stubFor("/trusts/match",requestBody, 200, """{"match": false}""")

          val result = Await.result(connector.checkExistingTrust(request), Duration.Inf)
          result mustBe ExistingTrustResponse.Success(false)
        }
      }
    }

  }


  def  stubFor(url :String,requestBody:String, returnStatus:Int, responseBody:String) =
    server.stubFor(post(urlEqualTo(url))
    .withHeader("content-Type", containing("application/json"))
      .withHeader("Environment", containing("dev"))
    .withRequestBody(equalTo(requestBody))
    .willReturn(
      aResponse()
        .withStatus(returnStatus)
        .withBody(responseBody)))
}
