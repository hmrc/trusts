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

package uk.gov.hmrc.trusts.controllers

import akka.util.Timeout
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.services.DesService
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.ExistingTrustCheckRequest
import org.mockito.Mockito._
import uk.gov.hmrc.trusts.models.ExistingTrustResponse.{AlreadyRegistered, Matched, NotMatched}
import org.mockito.Matchers.any

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DAYS, Duration, FiniteDuration, MINUTES}

class TrustsControllerSpec extends BaseSpec with GuiceOneServerPerSuite {


  val mockDesService = mock[DesService]
  val appConfig = mock[AppConfig]

  val validPayloadRequest = Json.parse("""{"name": "trust name","postcode": "NE11NE","utr": "1234567890"}""")
  val invalidPayloadRequest = Json.parse("""{"name": "","postcode": "NE11NE","utr": "12345678"}""")

  //implicit val timeout = new Timeout(new FiniteDuration(5, MINUTES))


  ".checkExistingTrust" should {

    "return OK with match true" when {
      "trusts data match with existing trusts. " in {
        val SUT = new TrustsController(mockDesService, appConfig)
        when(mockDesService.checkExistingTrust(any[ExistingTrustCheckRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Matched))
        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(validPayloadRequest))
        status(result) mustBe OK
        (contentAsJson(result) \ "match").as[Boolean] mustBe true
      }
    }

    "return OK with match false" when {
      "trusts data does not match with existing trusts." in {
        val SUT = new TrustsController(mockDesService, appConfig)
        when(mockDesService.checkExistingTrust(any[ExistingTrustCheckRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(NotMatched))

        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(validPayloadRequest))
        status(result) mustBe OK
        (contentAsJson(result) \ "match").as[Boolean] mustBe false
      }
    }

    "return 403 with message and code" when {
      "trusts data matched with already registered trusts." in {
        val SUT = new TrustsController(mockDesService, appConfig)
        when(mockDesService.checkExistingTrust(any[ExistingTrustCheckRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(AlreadyRegistered))

        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(validPayloadRequest))
        status(result) mustBe FORBIDDEN
        (contentAsJson(result) \ "code").as[String] mustBe "FORBIDDEN"
        (contentAsJson(result) \ "message").as[String] mustBe "The trust is already registered."
      }
    }

    "return 400 " when {
      "submitted payload is not valid" in {
        val SUT = new TrustsController(mockDesService, appConfig)
        when(mockDesService.checkExistingTrust(any[ExistingTrustCheckRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(NotMatched))
        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(invalidPayloadRequest))
        status(result) mustBe BAD_REQUEST
      }
    }


  } //checkExistingTrust
}

//end
