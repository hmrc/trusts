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

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{when, _}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.auth.core.{BearerTokenExpired, MissingBearerToken, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.connectors.{BaseSpec, FakeAuthConnector}
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.services.{AuthService, DesService, RosmPatternService, ValidationService}

import scala.concurrent.Future


class RegisterTrustControllerSpec extends BaseSpec with GuiceOneServerPerSuite {

  val mockDesService = mock[DesService]
  val rosmPatternService = mock[RosmPatternService]
  val authConnector = mock[AuthConnector]

  lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  lazy val validationService: ValidationService = new ValidationService()

  private val trnResponse = "XTRN123456"

  before {
    reset(rosmPatternService)
  }

  ".registration" should {

    "return 200 with TRN" when {
      "individual user called the register endpoint with a valid json payload " in {
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.successful(RegistrationTrnResponse(trnResponse)))
        when(rosmPatternService.completeRosmTransaction(Matchers.eq(trnResponse))(any[HeaderCarrier]))
          .thenReturn(Future.successful(TaxEnrolmentSuccess))
        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(organisationRetrieval)

        val mockAuthService = new AuthService(authConnector)
        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)


        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe OK
        (contentAsJson(result) \ "trn").as[String] mustBe trnResponse
        verify(rosmPatternService, times(1)).completeRosmTransaction(any())(any[HeaderCarrier])
      }

      "individual user called the register endpoint with a valid json payload " when {
        "tax enrolment failed to enrol user " in {
          when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
            .thenReturn(Future.successful(RegistrationTrnResponse(trnResponse)))
          when(rosmPatternService.completeRosmTransaction(Matchers.eq(trnResponse))(any[HeaderCarrier]))
            .thenReturn(Future.successful(TaxEnrolmentFailure))
          when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(organisationRetrieval)

          val mockAuthService = new AuthService(authConnector)
          val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService, rosmPatternService)


          val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
          status(result) mustBe OK
          (contentAsJson(result) \ "trn").as[String] mustBe trnResponse
          verify(rosmPatternService, times(1)).completeRosmTransaction(any())(any[HeaderCarrier])
        }
      }

      "agent user submits trusts payload" in {
        val agentRetrieval: Future[Option[AffinityGroup]] = Future.successful((Some(AffinityGroup.Agent)))

        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.successful(RegistrationTrnResponse(trnResponse)))

        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(agentRetrieval)

        val mockAuthService = new AuthService(authConnector)
        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)


        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe OK
        (contentAsJson(result) \ "trn").as[String] mustBe trnResponse
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }
    }

    "return Unauthorised" when {
      "the register endpoint is called user hasn't logged in" in {
        val mockAuthService = new AuthService(FakeAuthConnector(MissingBearerToken()))

        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)


        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe UNAUTHORIZED
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }

      "the register endpoint is called user session has expired" in {
        val mockAuthService = new AuthService(FakeAuthConnector(BearerTokenExpired()))
        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.successful(RegistrationTrnResponse(trnResponse)))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe UNAUTHORIZED
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }
    }

    "return a Conflict" when {
      "trusts is already registered with provided details." in {


        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(organisationRetrieval)

        val mockAuthService = new AuthService(authConnector)
        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(AlreadyRegisteredException))
        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe CONFLICT
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "ALREADY_REGISTERED"
        (output \ "message").as[String] mustBe "The trust is already registered."
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }
    }

    "return a Forbidden" when {
      "no match found for provided existing trusts details." in {

        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(organisationRetrieval)

        val mockAuthService = new AuthService(authConnector)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(NoMatchException))

        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe FORBIDDEN
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "NO_MATCH"
        (output \ "message").as[String] mustBe "No match has been found in HMRC's records."
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }
    }


    "return a BAD REQUEST" when {
      "input request fails schema validation" in {
        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(organisationRetrieval)

        val mockAuthService = new AuthService(authConnector)
        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(invalidRegistrationRequestJson)))
        status(result) mustBe BAD_REQUEST
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }

      "input request fails business validation" in {
        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(organisationRetrieval)

        val mockAuthService = new AuthService(authConnector)
        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(invalidTrustBusinessValidation)))
        status(result) mustBe BAD_REQUEST
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }

      "no draft id is provided in the headers" in {
        val agentRetrieval: Future[Option[AffinityGroup]] = Future.successful(Some(AffinityGroup.Agent))

        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.successful(RegistrationTrnResponse(trnResponse)))

        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(agentRetrieval)

        val mockAuthService = new AuthService(authConnector)
        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)


        val request = postRequestWithPayload(Json.parse(validRegistrationRequestJson), withDraftId = false)

        val result = SUT.registration().apply(request)

        status(result) mustBe BAD_REQUEST
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "NO_DRAFT_ID"
        (output \ "message").as[String] mustBe "No draft registration identifier provided."

        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }

    }


    "return an internal server error" when {
      "the register endpoint called and something goes wrong." in {

        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(InternalServerErrorException("some error")))

        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(organisationRetrieval)
        val mockAuthService = new AuthService(authConnector)
        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }
    }

    "return an internal server error" when {
      "the des returns BAD REQUEST" in {
        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(organisationRetrieval)
        val mockAuthService = new AuthService(authConnector)

        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier])).
          thenReturn(Future.failed(BadRequestException))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }
    }

    "return an internal server error" when {
      "the des returns Service Unavailable as dependent service is down. " in {
        when(authConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any())).thenReturn(organisationRetrieval)
        val mockAuthService = new AuthService(authConnector)

        val SUT = new RegisterTrustController(mockDesService, appConfig, validationService, mockAuthService,rosmPatternService)
        when(mockDesService.registerTrust(any[Registration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(ServiceNotAvailableException("dependent service is down")))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistrationRequestJson)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
        verify(rosmPatternService, times(0)).completeRosmTransaction(any())(any[HeaderCarrier])
      }
    }
  }
}
