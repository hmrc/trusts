/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.testOnly

import java.util.UUID

import base.BaseSpec
import org.mockito.Matchers.{eq => Meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import config.AppConfig
import controllers._
import controllers.actions.FakeIdentifierAction
import exceptions._
import models.variation.VariationResponse
import services._
import utils.Headers

import scala.concurrent.{ExecutionContext, Future}

class TrustVariationsTestControllerSpec extends BaseSpec with BeforeAndAfter with BeforeAndAfterEach  with IntegrationPatience {

  private lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private lazy val mockTrustsService: TrustsService = mock[TrustsService]

  private lazy val mockAuditService: AuditService = mock[AuditService]

  private val mockAuditConnector: AuditConnector = mock[AuditConnector]
  private val mockConfig: AppConfig = mock[AppConfig]

  private val auditService = new AuditService(mockAuditConnector)
  private val validationService = new ValidationService()

  private val mockVariationService = mock[VariationService]

  private val mockTrustsStoreService = mock[TrustsStoreService]

  private val responseHandler = new VariationsResponseHandler(mockAuditService)

  override def beforeEach(): Unit = {
    reset(mockTrustsService, mockAuditService, mockAuditConnector, mockConfig, mockTrustsStoreService)
    when(mockConfig.variationsApiSchema4MLD).thenReturn(appConfig.variationsApiSchema4MLD)
    when(mockConfig.variationsApiSchema5MLD).thenReturn(appConfig.variationsApiSchema5MLD)
    when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(false))
  }

  private def trustVariationsController = {
    new TrustVariationsTestController(
      new FakeIdentifierAction(bodyParsers, Organisation),
      mockTrustsService,
      mockAuditService,
      validationService,
      mockConfig,
      mockVariationService,
      responseHandler,
      mockTrustsStoreService,
      Helpers.stubControllerComponents()
    )
  }

  val tvnResponse = "XXTVN1234567890"
  val trustVariationsAuditEvent = "TrustVariation"

  ".trustVariation" should {

    "not perform auditing" when {
      "the feature toggle is set to false" in {

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        val requestPayLoad = Json.parse(validTrustVariations4mldRequestJson)

        val SUT = trustVariationsController

        val result = SUT.trustVariation()(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result) { _ =>

          verify(mockAuditConnector, times(0)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "perform auditing" when {

      "the feature toggle is set to true" in {

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        val requestPayLoad = Json.parse(validTrustVariations4mldRequestJson)

        val SUT = new TrustVariationsTestController(
          new FakeIdentifierAction(bodyParsers, Organisation),
          mockTrustsService,
          auditService,
          validationService,
          mockConfig,
          mockVariationService,
          responseHandler,
          mockTrustsStoreService,
          Helpers.stubControllerComponents())

        val result = SUT.trustVariation()(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result) { _ =>

          verify(mockAuditConnector, times(1)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "return 200 with TVN" when {

      "individual user called the register endpoint with a valid json payload " in {

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        val requestPayLoad = Json.parse(validTrustVariations4mldRequestJson)

        val SUT = trustVariationsController

        val result = SUT.trustVariation()(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe OK

        verify(mockAuditService).audit(
          Meq(trustVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq(Json.obj("tvn" -> tvnResponse))
        )(any())

        (contentAsJson(result) \ "tvn").as[String] mustBe tvnResponse

      }

      "individual user called the register endpoint with a valid 5mld json payload in 5mld mode" in {

        when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(true))

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        val requestPayLoad = Json.parse(validTrustVariations5mldRequestJson)

        val SUT = trustVariationsController

        val result = SUT.trustVariation()(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe OK

        verify(mockAuditService).audit(
          Meq(trustVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq(Json.obj("tvn" -> tvnResponse))
        )(any())

        (contentAsJson(result) \ "tvn").as[String] mustBe tvnResponse
      }

      "valid 5mld json payload with tax years in 5mld mode" in {

        when(mockTrustsStoreService.is5mldEnabled()(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(true))

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        val requestPayLoad = Json.parse(validTrustVariationsTaxYears5mldRequestJson)

        val SUT = trustVariationsController

        val result = SUT.trustVariation()(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe OK

        verify(mockAuditService).audit(
          Meq(trustVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq(Json.obj("tvn" -> tvnResponse))
        )(any())

        (contentAsJson(result) \ "tvn").as[String] mustBe tvnResponse
      }
    }

    "return a BadRequest" when {

      "input request fails schema validation" in {

        val SUT = trustVariationsController

        val result = SUT.trustVariation()(
          postRequestWithPayload(Json.parse(invalidTrustVariationsRequestJson), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe BAD_REQUEST

        verify(mockAuditService).auditErrorResponse(
          Meq(trustVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Provided request is invalid.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."

      }

      "input request fails business validation" in {

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.failed(BadRequestException))

        val SUT = trustVariationsController

        val result = SUT.trustVariation()(
          postRequestWithPayload(Json.parse(invalidTrustBusinessValidation), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe BAD_REQUEST

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."

      }

      "invalid correlation id is provided in the headers" in {

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.failed(InvalidCorrelationIdException))

        val SUT = trustVariationsController

        val request = postRequestWithPayload(Json.parse(validTrustVariations4mldRequestJson), withDraftId = false)

        val result = SUT.trustVariation()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR

        verify(mockAuditService).auditErrorResponse(
          Meq(trustVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Submission has not passed validation. Invalid CorrelationId.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "INVALID_CORRELATIONID"
        (output \ "message").as[String] mustBe "Submission has not passed validation. Invalid CorrelationId."

      }

    }

    "return a Conflict" when {
      "submission with same correlation id is submitted." in {

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.failed(DuplicateSubmissionException))

        val SUT = trustVariationsController

        val result = SUT.trustVariation()(
          postRequestWithPayload(Json.parse(validTrustVariations4mldRequestJson), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe CONFLICT

        verify(mockAuditService).auditErrorResponse(
          Meq(trustVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Duplicate Correlation Id was submitted.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "DUPLICATE_SUBMISSION"
        (output \ "message").as[String] mustBe "Duplicate Correlation Id was submitted."

      }
    }

    "return an internal server error" when {

      "the register endpoint called and something goes wrong." in {

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.failed(InternalServerErrorException("some error")))

        val SUT = trustVariationsController

        val result = SUT.trustVariation()(
          postRequestWithPayload(Json.parse(validTrustVariations4mldRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe INTERNAL_SERVER_ERROR

        verify(mockAuditService).auditErrorResponse(
          Meq(trustVariationsAuditEvent),
          any(),
          Meq("id"),
          any()
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."

      }

    }
    "return service unavailable" when {
      "the des returns Service Unavailable as dependent service is down. " in {

        when(mockTrustsService.trustVariation(any[JsValue]))
          .thenReturn(Future.failed(ServiceNotAvailableException("dependent service is down")))

        val SUT = trustVariationsController

        val result = SUT.trustVariation()(
          postRequestWithPayload(Json.parse(validTrustVariations4mldRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe SERVICE_UNAVAILABLE

        verify(mockAuditService).auditErrorResponse(
          Meq(trustVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Service unavailable.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "SERVICE_UNAVAILABLE"
        (output \ "message").as[String] mustBe "Service unavailable."

      }
    }
  }
}

