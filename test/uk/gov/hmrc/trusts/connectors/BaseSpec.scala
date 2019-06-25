/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, MustMatchers, WordSpec}
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.utils.{Headers, JsonRequests}

import scala.concurrent.Future

class BaseSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar with JsonRequests with BeforeAndAfter {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val organisationRetrieval: Future[Option[AffinityGroup]] = Future.successful((Some(AffinityGroup.Organisation)))

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  def postRequestWithPayload(payload: JsValue, withDraftId : Boolean = true): FakeRequest[JsValue] = {
    if (withDraftId) {
      FakeRequest("POST", "/trusts/register")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withHeaders(Headers.DraftRegistrationId -> UUID.randomUUID().toString)
        .withBody(payload)
    } else {
      FakeRequest("POST", "/trusts/register")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody(payload)
    }
  }


    def stubForPost(server: WireMockServer,
                url: String,
                requestBody: String,
                returnStatus: Int,
                responseBody: String,
                delayResponse: Int = 0) = {

      server.stubFor(post(urlEqualTo(url))
        .withHeader(CONTENT_TYPE, containing("application/json"))
        .withHeader("Environment", containing("dev"))
        .withRequestBody(equalTo(requestBody))
        .willReturn(
          aResponse()
            .withStatus(returnStatus)
            .withBody(responseBody).withFixedDelay(delayResponse)))
    }


    def stubForGet(server: WireMockServer,
                   url: String, returnStatus: Int,
                   responseBody: String,
                   delayResponse: Int = 0) = {
      server.stubFor(get(urlEqualTo(url))
        .withHeader("content-Type", containing("application/json"))
        .willReturn(
          aResponse()
            .withStatus(returnStatus)
            .withBody(responseBody).withFixedDelay(delayResponse)))
    }

    def stubForPut(server: WireMockServer,
                   url: String,
                   returnStatus: Int,
                   delayResponse: Int = 0) = {
      server.stubFor(put(urlEqualTo(url))
        .withHeader("content-Type", containing("application/json"))
        .willReturn(
          aResponse()
            .withStatus(returnStatus)
            .withFixedDelay(delayResponse)))
    }


}






