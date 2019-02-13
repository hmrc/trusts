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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers.status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.services.{AuthService, DesService, ValidationService}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models._
import org.mockito.Mockito._
import uk.gov.hmrc.auth.core.{AuthorisationException, BearerTokenExpired, MissingBearerToken}
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate

import scala.concurrent.{ExecutionContext, Future}


class RegisterTrustControllerSpec extends BaseSpec with GuiceOneServerPerSuite {

  val mockDesService = mock[DesService]

  lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  lazy val validatationService: ValidationService = new ValidationService()

  ".registration" should {

    "return 200 with TRN" when {
      "the register endpoint is called with a valid json payload " in {
        val mockAuthService = new AuthService(authConnector())
        val SUT = new RegisterTrustController(mockDesService, appConfig, validatationService, mockAuthService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.successful(RegistrationTrustResponse("XTRN123456")))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe OK
        (contentAsJson(result) \ "trn").as[String] mustBe "XTRN123456"
      }
    }

    "return Unauthorised" when {
      "the register endpoint is called user hasn't logged in" in {
        val mockAuthService = new AuthService(authConnector(Some(MissingBearerToken())))
        val SUT = new RegisterTrustController(mockDesService, appConfig, validatationService, mockAuthService)


        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe UNAUTHORIZED
      }

      "the register endpoint is called user session has expired" in {
        val mockAuthService = new AuthService(authConnector(Some(BearerTokenExpired())))
        val SUT = new RegisterTrustController(mockDesService, appConfig, validatationService, mockAuthService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.successful(RegistrationTrustResponse("XTRN123456")))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return a Conflict" when {
      "trusts is already registered with provided details." in {
        val mockAuthService = new AuthService(authConnector())
        val SUT = new RegisterTrustController(mockDesService, appConfig, validatationService, mockAuthService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new AlreadyRegisteredException))
        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe CONFLICT
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "ALREADY_REGISTERED"
        (output \ "message").as[String] mustBe "The trust is already registered."
      }
    }

    "return a Forbidden" when {
      "no match found for provided existing trusts details." in {
        val mockAuthService = new AuthService(authConnector())

        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new NoMatchException))

        val SUT = new RegisterTrustController(mockDesService, appConfig, validatationService, mockAuthService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe FORBIDDEN
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "NO_MATCH"
        (output \ "message").as[String] mustBe "No match has been found in HMRC's records."
      }
    }


    "return a BAD REQUEST" when {
      "input request fails schema validation" in {
        val mockAuthService = new AuthService(authConnector())
        val SUT = new RegisterTrustController(mockDesService, appConfig, validatationService, mockAuthService)
        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(invalidRegistrationRequestJson)))
        status(result) mustBe BAD_REQUEST
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."
      }
    }


    "return an internal server error" when {
      "the register endpoint called and something goes wrong." in {

        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new InternalServerErrorException("some error")))

        val mockAuthService = new AuthService(authConnector())
        val SUT = new RegisterTrustController(mockDesService, appConfig, validatationService, mockAuthService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }
    }

    "return an internal server error" when {
      "the des returns BAD REQUEST" in {
        val mockAuthService = new AuthService(authConnector())
        val SUT = new RegisterTrustController(mockDesService, appConfig, validatationService, mockAuthService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new BadRequestException))
        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }
    }

    "return an internal server error" when {
      "the des returns Service Unavailable as dependent service is down. " in {
        val mockAuthService = new AuthService(authConnector())
        val SUT = new RegisterTrustController(mockDesService, appConfig, validatationService, mockAuthService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new ServiceNotAvailableException("dependent service is down")))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
      }
    }
  }
}
