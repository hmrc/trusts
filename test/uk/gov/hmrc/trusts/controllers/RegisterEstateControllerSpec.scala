/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, _}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.services.{DesService, FakeAuditService, RosmPatternService, ValidationService}

import scala.concurrent.Future


class RegisterEstateControllerSpec extends BaseSpec with GuiceOneServerPerSuite {

  private val mockDesService = mock[DesService]
  private val rosmPatternService = mock[RosmPatternService]

  val fakeOrganisationAuthAction = new FakeIdentifierAction(Organisation)
  val fakeAgentAuthAction = new FakeIdentifierAction(Agent)

  lazy val validationService: ValidationService = new ValidationService()

  lazy val mockedAuditService = injector.instanceOf[FakeAuditService]

  private val estateTrnResponse = "XTRN123456"

  before {
    reset(rosmPatternService)
  }

  ".registration of estate " should {

    "return 200 with TRN" when {

      "individual user called the register endpoint with a valid json payload " in {
        mockRosmResponse(TaxEnrolmentSuccess)
        mockDesServiceResponse

        val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeOrganisationAuthAction, rosmPatternService, mockedAuditService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(estateRegistration01)))
        status(result) mustBe OK
        (contentAsJson(result) \ "trn").as[String] mustBe estateTrnResponse
        verify(rosmPatternService, times(1)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
      }

      "individual user called the register endpoint with a valid json payload " when {

        "tax enrolment failed to enrol user " in {
          mockRosmResponse(TaxEnrolmentFailure)
          mockDesServiceResponse

          val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeOrganisationAuthAction, rosmPatternService, mockedAuditService)

          val result = SUT.registration().apply(postRequestWithPayload(Json.parse(estateRegistration01)))
          status(result) mustBe OK
          (contentAsJson(result) \ "trn").as[String] mustBe estateTrnResponse
          verify(rosmPatternService, times(1)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
        }
      }

      "agent user submits estates payload" in {
        mockRosmResponse(TaxEnrolmentSuccess)
        mockDesServiceResponse

        val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeAgentAuthAction,rosmPatternService, mockedAuditService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(estateRegistration03)))
        status(result) mustBe OK
        (contentAsJson(result) \ "trn").as[String] mustBe estateTrnResponse

        verify(rosmPatternService, times(1)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
      }
    }

    "return a Conflict" when {

      "estates is already registered with provided details." in {
        when(mockDesService.registerEstate(any[EstateRegistration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(AlreadyRegisteredException))

        val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeOrganisationAuthAction,rosmPatternService, mockedAuditService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(estateRegistration01)))
        status(result) mustBe CONFLICT
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "ALREADY_REGISTERED"
        (output \ "message").as[String] mustBe "The estate is already registered."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
      }
    }

    "return a Forbidden" when {
      "no match found for provided existing estate details." in {

        when(mockDesService.registerEstate(any[EstateRegistration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(NoMatchException))

        val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeOrganisationAuthAction,rosmPatternService, mockedAuditService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(estateRegistration01)))
        status(result) mustBe FORBIDDEN
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "NO_MATCH"
        (output \ "message").as[String] mustBe "No match has been found in HMRC's records."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
      }
    }


    "return a BAD REQUEST" when {
      "input request fails schema validation" in {

        val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeOrganisationAuthAction,rosmPatternService, mockedAuditService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(invalidRegistrationRequestJson)))
        status(result) mustBe BAD_REQUEST
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
      }

      "no draft id provided in headers" in {

        mockDesServiceResponse

        val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeOrganisationAuthAction,rosmPatternService, mockedAuditService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(estateRegistration03), withDraftId = false))

        status(result) mustBe BAD_REQUEST
        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "NO_DRAFT_ID"
        (output \ "message").as[String] mustBe "No draft registration identifier provided."

        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
      }

    }


    "return an internal server error" when {
      "the register endpoint called and something goes wrong." in {
        when(mockDesService.registerEstate(any[EstateRegistration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(InternalServerErrorException("some error")))

        val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeOrganisationAuthAction,rosmPatternService, mockedAuditService)

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(estateRegistration01)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
      }
    }

    "return an internal server error" when {
      "the des returns BAD REQUEST" in {
        val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeOrganisationAuthAction,rosmPatternService, mockedAuditService)
        when(mockDesService.registerEstate(any[EstateRegistration])(any[HeaderCarrier])).
          thenReturn(Future.failed(BadRequestException))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(estateRegistration01)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
      }
    }

    "return an internal server error" when {
      "the des returns Service Unavailable as dependent service is down. " in {

        val SUT = new RegisterEstateController(mockDesService, appConfig, validationService, fakeOrganisationAuthAction,rosmPatternService, mockedAuditService)
        when(mockDesService.registerEstate(any[EstateRegistration])(any[HeaderCarrier]))
          .thenReturn(Future.failed(ServiceNotAvailableException("dependent service is down")))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(estateRegistration01)))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any())(any[HeaderCarrier])
      }
    }
  }

  private def mockDesServiceResponse = {
    when(mockDesService.registerEstate(any[EstateRegistration])(any[HeaderCarrier]))
      .thenReturn(Future.successful(RegistrationTrnResponse(estateTrnResponse)))
  }

  private def mockRosmResponse(response : TaxEnrolmentSuscriberResponse) = {
    when(rosmPatternService.enrolAndLogResult(ArgumentMatchers.eq(estateTrnResponse), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(response))
  }
}
