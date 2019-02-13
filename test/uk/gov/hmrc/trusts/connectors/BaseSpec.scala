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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.Mockito.when
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.utils.JsonRequests
import play.api.test
import org.scalatest.time.{Millis, Seconds, Span}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.trusts.services.AuthService
import org.mockito.Mockito._
import org.mockito.Matchers

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, Retrieval, ~}

class BaseSpec extends WordSpec with MustMatchers with ScalaFutures with MockitoSugar with JsonRequests  {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  def postRequestWithPayload(payload: JsValue): FakeRequest[JsValue] =
    FakeRequest("POST", "/trusts/register")
      .withHeaders(CONTENT_TYPE -> "application/json")
      .withBody(payload)


  def stubFor(server: WireMockServer ,  url: String, requestBody: String, returnStatus: Int, responseBody: String, delayResponse: Int = 0) = {
    server.stubFor(post(urlEqualTo(url))
      .withHeader("content-Type", containing("application/json"))
      .withHeader("Environment", containing("dev"))
      .withRequestBody(equalTo(requestBody))
      .willReturn(
        aResponse()
          .withStatus(returnStatus)
          .withBody(responseBody).withFixedDelay(delayResponse)))
  }


  def authConnector( exception: Option[AuthorisationException]= None): AuthConnector = {
    val success: Any = ()
    new AuthConnector {
      def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
        exception.fold(Future.successful(success.asInstanceOf[A]))(Future.failed(_))
      }
    }
  }

}
