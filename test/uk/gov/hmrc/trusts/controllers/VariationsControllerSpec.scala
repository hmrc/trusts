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

import java.util.UUID

import org.mockito.Matchers._
import org.mockito.Matchers.{eq => Meq}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.variation.{Variation, VariationResponse}
import uk.gov.hmrc.trusts.services.{AuditService, DesService}
import uk.gov.hmrc.trusts.utils.Headers

import scala.concurrent.Future

class VariationsControllerSpec extends BaseSpec {

  lazy val mockDesService: DesService = mock[DesService]

  lazy val mockAuditService: AuditService = mock[AuditService]

  private def variationsController = {
    val SUT = new VariationsController(new FakeIdentifierAction(Organisation), mockDesService, mockAuditService)
    SUT
  }

  val tvnResponse = "XXTVN1234567890"
  val trustVariationsAuditEvent =  "TrustVariation"

  ".variation" should {

    "return 200 with TVN" when {

      "individual user called the register endpoint with a valid json payload " in {

        when(mockDesService.variation(any[Variation])(any[HeaderCarrier]))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        val requestPayLoad = Json.parse(validVariationsRequestJson)

        val SUT = variationsController

        val result = SUT.variation()(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result){_ =>

          verify(mockAuditService).audit(
            Meq(trustVariationsAuditEvent),
            any(),
            Meq("id"),
            Meq(Json.obj("tvn" -> tvnResponse))
          )(any())
        }

        status(result) mustBe OK
        (contentAsJson(result) \ "tvn").as[String] mustBe tvnResponse

      }

    }

    "return a BadRequest" when {

      "input request fails schema validation" in {

        val SUT = variationsController

        val result = SUT.variation()(
          postRequestWithPayload(Json.parse(invalidVariationsRequestJson), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result){_ =>

          verify(mockAuditService).auditErrorResponse(
            Meq(trustVariationsAuditEvent),
            any(),
            Meq("id"),
            Meq("Provided request is invalid.")
          )(any())
        }

        status(result) mustBe BAD_REQUEST

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."

      }

      "input request fails business validation" in {

        when(mockDesService.variation(any[Variation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(BadRequestException))

        val SUT = variationsController

        val result = SUT.variation()(
          postRequestWithPayload(Json.parse(invalidTrustBusinessValidation), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe BAD_REQUEST

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."

      }

      "invalid correlation id is provided in the headers" in {

        when(mockDesService.variation(any[Variation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(InvalidCorrelationIdException))

        val SUT = variationsController

        val request = postRequestWithPayload(Json.parse(validVariationsRequestJson), withDraftId = false)

        val result = SUT.variation()(request)

        whenReady(result){_ =>

          verify(mockAuditService).auditErrorResponse(
            Meq(trustVariationsAuditEvent),
            any(),
            Meq("id"),
            Meq("Submission has not passed validation. Invalid CorrelationId.")
          )(any())
        }

        status(result) mustBe INTERNAL_SERVER_ERROR

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "INVALID_CORRELATIONID"
        (output \ "message").as[String] mustBe "Submission has not passed validation. Invalid CorrelationId."

      }

    }

    "return a Conflict" when {
      "submission with same correlation id is submitted." in {

        when(mockDesService.variation(any[Variation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(DuplicateSubmissionException))

        val SUT = variationsController

        val result = SUT.variation()(
          postRequestWithPayload(Json.parse(validVariationsRequestJson), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result){_ =>

          verify(mockAuditService).auditErrorResponse(
            Meq(trustVariationsAuditEvent),
            any(),
            Meq("id"),
            Meq("Duplicate Correlation Id was submitted.")
          )(any())
        }

        status(result) mustBe CONFLICT

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "DUPLICATE_SUBMISSION"
        (output \ "message").as[String] mustBe "Duplicate Correlation Id was submitted."

      }
    }

    "return an internal server error" when {

      "the register endpoint called and something goes wrong." in {

        when(mockDesService.variation(any[Variation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(InternalServerErrorException("some error")))

        val SUT = variationsController

        val result = SUT.variation()(
          postRequestWithPayload(Json.parse(validVariationsRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )
        whenReady(result){_ =>

          verify(mockAuditService).auditErrorResponse(
            Meq(trustVariationsAuditEvent),
            any(),
            Meq("id"),
            Meq("Internal server error.")
          )(any())
        }

        status(result) mustBe INTERNAL_SERVER_ERROR

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."

      }

    }

    "return service unavailable" when {
      "the des returns Service Unavailable as dependent service is down. " in {

        when(mockDesService.variation(any[Variation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(ServiceNotAvailableException("dependent service is down")))

        val SUT = variationsController

        val result = SUT.variation()(
          postRequestWithPayload(Json.parse(validVariationsRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result){_ =>

          verify(mockAuditService).auditErrorResponse(
            Meq(trustVariationsAuditEvent),
            any(),
            Meq("id"),
            Meq("Service unavailable.")
          )(any())
        }


        status(result) mustBe SERVICE_UNAVAILABLE

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "SERVICE_UNAVAILABLE"
        (output \ "message").as[String] mustBe "Service unavailable."

      }
    }
  }
}
