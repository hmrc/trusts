/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import base.BaseSpec
import controllers.actions.FakeIdentifierAction
import models._
import models.nonRepudiation.NRSResponse
import models.registration._
import models.tax_enrolments.{TaxEnrolmentFailure, TaxEnrolmentNotProcessed, TaxEnrolmentSuccess}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, _}
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.test.Helpers
import play.api.test.Helpers.{status, _}
import services._
import services.nonRepudiation.NonRepudiationService
import services.rosm.RosmPatternService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class RegisterTrustControllerSpec extends BaseSpec {

  private val mockTrustsService: TrustsService = mock[TrustsService]
  private val mockNonRepudiationService: NonRepudiationService = mock[NonRepudiationService]
  private val rosmPatternService: RosmPatternService = mock[RosmPatternService]
  private val default5mldDataService: AmendSubmissionDataService = injector.instanceOf[AmendSubmissionDataService]
  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val fakeOrganisationAuthAction = new FakeIdentifierAction(bodyParsers, Organisation)
  private val fakeAgentAuthAction = new FakeIdentifierAction(bodyParsers, Agent)

  private lazy val validationService: ValidationService = new ValidationService()

  private val trnResponse = "XTRN123456"

  before {
    reset(rosmPatternService)
  }

  ".registration" should {

    "return 200 with TRN" when {

      "individual user called the register endpoint with a valid 5mld json payload " in {

        when(mockTrustsService.registerTrust(any[Registration]))
          .thenReturn(Future.successful(RegistrationTrnResponse(trnResponse)))

        when(mockNonRepudiationService.register(any(), any())(any(), any()))
          .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

        when(rosmPatternService.enrolAndLogResult(any(), any(), any())(any())).thenReturn(Future.successful(TaxEnrolmentSuccess))

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistration5MldRequestJson)))
        status(result) mustBe OK
        (contentAsJson(result) \ "trn").as[String] mustBe trnResponse

        verify(rosmPatternService, times(1)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }

      "individual user called the register endpoint with a valid 5mld nontaxable json payload " in {

        when(mockTrustsService.registerTrust(any[Registration]))
          .thenReturn(Future.successful(RegistrationTrnResponse(trnResponse)))

        when(mockNonRepudiationService.register(any(), any())(any(), any()))
          .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

        when(rosmPatternService.enrolAndLogResult(any(), any(), any())(any())).thenReturn(Future.successful(TaxEnrolmentSuccess))

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistration5MldNontaxableRequestJson)))
        status(result) mustBe OK
        (contentAsJson(result) \ "trn").as[String] mustBe trnResponse

        verify(rosmPatternService, times(1)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }

      "individual user called the register endpoint with a valid json payload " when {

        "tax enrolment failed to enrol user" in {

          when(mockTrustsService.registerTrust(any[Registration]))
            .thenReturn(Future.successful(RegistrationTrnResponse(trnResponse)))

          when(mockNonRepudiationService.register(any(), any())(any(), any()))
            .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

          when(rosmPatternService.enrolAndLogResult(any(), any(), any())(any())).thenReturn(Future.successful(TaxEnrolmentFailure))

          val SUT = new RegisterTrustController(
            mockTrustsService,
            appConfig,
            validationService,
            fakeOrganisationAuthAction,
            rosmPatternService,
            mockNonRepudiationService,
            Helpers.stubControllerComponents(),
            default5mldDataService
          )

          val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistration5MldRequestJson)))
          status(result) mustBe OK
          (contentAsJson(result) \ "trn").as[String] mustBe trnResponse

          verify(rosmPatternService, times(1)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
        }
      }

      "agent user submits trusts payload" in {

        when(mockNonRepudiationService.register(any(), any())(any(), any()))
          .thenReturn(Future.successful(NRSResponse.Success("2880d8aa-4691-49a4-aa6a-99191a51b9ef")))

        when(rosmPatternService.enrolAndLogResult(any(), any(), any())(any())).thenReturn(Future.successful(TaxEnrolmentNotProcessed))

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeAgentAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistration5MldRequestJson)))

        status(result) mustBe OK

        (contentAsJson(result) \ "trn").as[String] mustBe trnResponse

        verify(rosmPatternService, times(1)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }
    }

    "return a Conflict" when {
      "trusts is already registered with provided details." in {

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        when(mockTrustsService.registerTrust(any[Registration]))
          .thenReturn(Future.successful(AlreadyRegisteredResponse))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistration5MldRequestJson)))

        status(result) mustBe CONFLICT

        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "ALREADY_REGISTERED"
        (output \ "message").as[String] mustBe "The trust is already registered."

        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }
    }

    "return a Forbidden" when {

      "no match found for provided existing trusts details." in {
        when(mockTrustsService.registerTrust(any[Registration]))
          .thenReturn(Future.successful(NoMatchResponse))

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistration5MldRequestJson)))
        status(result) mustBe FORBIDDEN
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "NO_MATCH"
        (output \ "message").as[String] mustBe "No match has been found in HMRC's records."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }
    }

    "return a BAD REQUEST" when {

      "input request fails schema validation" in {

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(invalidRegistrationRequestJson)))

        status(result) mustBe BAD_REQUEST

        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."

        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }

      "input request fails business validation" in {

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(invalidTrustBusinessValidation)))
        status(result) mustBe BAD_REQUEST
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }

      "no draft id is provided in the headers" in {

        when(mockTrustsService.registerTrust(any[Registration]))
          .thenReturn(Future.successful(RegistrationTrnResponse(trnResponse)))

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        val request = postRequestWithPayload(Json.parse(validRegistration5MldRequestJson), withDraftId = false)

        val result = SUT.registration().apply(request)

        status(result) mustBe BAD_REQUEST
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "NO_DRAFT_ID"
        (output \ "message").as[String] mustBe "No draft registration identifier provided."

        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }

    }

    "return an internal server error" when {

      "the register endpoint called and something goes wrong." in {

        when(mockTrustsService.registerTrust(any[Registration]))
          .thenReturn(Future.successful(InternalServerErrorResponse))

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistration5MldRequestJson)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }
    }

    "return an internal server error" when {

      "the des returns BAD REQUEST" in {

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        when(mockTrustsService.registerTrust(any[Registration])).
          thenReturn(Future.successful(BadRequestResponse))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistration5MldRequestJson)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }
    }

    "return an internal server error" when {

      "the des returns Service Unavailable as dependent service is down. " in {

        val SUT = new RegisterTrustController(
          mockTrustsService,
          appConfig,
          validationService,
          fakeOrganisationAuthAction,
          rosmPatternService,
          mockNonRepudiationService,
          Helpers.stubControllerComponents(),
          default5mldDataService
        )

        when(mockTrustsService.registerTrust(any[Registration]))
          .thenReturn(Future.successful(ServiceUnavailableResponse))

        val result = SUT.registration().apply(postRequestWithPayload(Json.parse(validRegistration5MldRequestJson)))

        status(result) mustBe INTERNAL_SERVER_ERROR
        val output = contentAsJson(result)
        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."
        verify(rosmPatternService, times(0)).enrolAndLogResult(any(), any(), any())(any[HeaderCarrier])
      }
    }
  }
}
