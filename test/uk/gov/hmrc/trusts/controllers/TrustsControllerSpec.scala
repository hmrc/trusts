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
import uk.gov.hmrc.trusts.models.ExistingTrustResponse.{AlreadyRegistered, Matched, NotMatched, ServiceUnavailable}
import org.mockito.Matchers.any

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DAYS, Duration, FiniteDuration, MINUTES}

class TrustsControllerSpec extends BaseSpec with GuiceOneServerPerSuite {


  val mockDesService = mock[DesService]
  val appConfig = mock[AppConfig]

  val validPayloadRequest = Json.parse("""{"name": "trust name","postcode": "NE1 1NE","utr": "1234567890"}""")
  val validPayloadRequestWithoutPostCode = Json.parse("""{"name": "trust name","utr": "1234567890"}""")




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

    "return OK with match true" when {
      "trusts data match with existing trusts without postcode. " in {
        val SUT = new TrustsController(mockDesService, appConfig)
        when(mockDesService.checkExistingTrust(any[ExistingTrustCheckRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Matched))
        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(validPayloadRequestWithoutPostCode))
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
      "trust name is not valid" in {
        val SUT = new TrustsController(mockDesService, appConfig)
        val nameInvalidPayload = Json.parse("""{"name": "","postcode": "NE11NE","utr": "1234567890"}""")

        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(nameInvalidPayload))
        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "code").as[String] mustBe "INVALID_NAME"
        (contentAsJson(result) \ "message").as[String] mustBe "Provided name is invalid."
      }

      "trust name is more than 56 characters" in {
        val SUT = new TrustsController(mockDesService, appConfig)
        val nameInvalidPayload = Json.parse("""{"name": "Lorem ipsum dolor sit amet, consectetur adipiscing elitee","postcode": "NE11NE","utr": "1234567890"}""")

        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(nameInvalidPayload))
        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "code").as[String] mustBe "INVALID_NAME"
        (contentAsJson(result) \ "message").as[String] mustBe "Provided name is invalid."
      }


    }

    "return 400 " when {
      "utr is not valid" in {
        val SUT = new TrustsController(mockDesService, appConfig)

        val utrInvalidPayload = Json.parse("""{"name": "trust name","postcode": "NE11NE","utr": "12345678"}""")
        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(utrInvalidPayload))
        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "code").as[String] mustBe "INVALID_UTR"
        (contentAsJson(result) \ "message").as[String] mustBe "Provided utr is invalid."
      }
    }

    "return 400 " when {
      "postcode is not valid" in {
        val SUT = new TrustsController(mockDesService, appConfig)

        val invalidPayload = Json.parse("""{"name": "trust name","postcode": "AA9A 9AAT","utr": "1234567890"}""")

        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(invalidPayload))
        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "code").as[String] mustBe "INVALID_POSTCODE"
        (contentAsJson(result) \ "message").as[String] mustBe "Provided postcode is invalid."
      }
    }

    "return 400 " when {
      "request is not valid" in {
        val SUT = new TrustsController(mockDesService, appConfig)

        val requestInvalid = Json.parse("""{"name1": "trust name","postcode": "NE11NE","utr": "1234567890"}""")

        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(requestInvalid))
        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "code").as[String] mustBe "BAD_REQUEST"
        (contentAsJson(result) \ "message").as[String] mustBe "Provided request is invalid."
      }
    }

    "return Internal server error " when {
      "des dependent service is not responding" in {
        val SUT = new TrustsController(mockDesService, appConfig)
        when(mockDesService.checkExistingTrust(any[ExistingTrustCheckRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(ServiceUnavailable))

        val result = SUT.checkExistingTrust().apply(postRequestWithPayload(validPayloadRequest))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }
    }


  } //checkExistingTrust
}

//end
