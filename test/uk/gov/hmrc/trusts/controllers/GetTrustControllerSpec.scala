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

import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.get_trust_or_estate._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{TrustFoundResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.services.{AuditService, DesService, TransformationService}
import uk.gov.hmrc.trusts.utils.JsonRequests

import scala.concurrent.Future

class GetTrustControllerSpec extends WordSpec with MockitoSugar with MustMatchers with BeforeAndAfter with BeforeAndAfterEach with JsonRequests with Inside with ScalaFutures {

  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private val desService: DesService = mock[DesService]
  private val mockedAuditService: AuditService = mock[AuditService]

  private val mockAuditConnector = mock[AuditConnector]
  private val mockConfig = mock[AppConfig]

  private val transformationService = mock[TransformationService]

  private val auditService = new AuditService(mockAuditConnector, mockConfig)

  override def afterEach(): Unit =  {
    reset(mockedAuditService, desService, mockAuditConnector, mockConfig, transformationService)
  }

  private def getTrustController = {
    val SUT = new GetTrustController(new FakeIdentifierAction(Organisation), mockedAuditService, desService, transformationService)
    SUT
  }

  val invalidUTR = "1234567"
  val utr = "1234567890"

  ".getFromEtmp" should {
    "reset the cache and clear transforms before calling get" in {

      val SUT = new GetTrustController(new FakeIdentifierAction(Organisation), auditService, desService, transformationService)

      val response = TrustFoundResponse(ResponseHeader("Parked", "1"))
      when(desService.resetCache(any(), any()))
        .thenReturn(Future.successful(()))

      when(transformationService.removeAllTransformations(any(), any()))
        .thenReturn(Future.successful(None))

      when(desService.getTrustInfo(any(), any())(any()))
        .thenReturn(Future.successful(response))

      val result = SUT.getFromEtmp(utr).apply(FakeRequest(GET, s"/trusts/$utr/refresh"))

      whenReady(result) { _ =>
        verify(desService).resetCache(utr, "id")
        verify(transformationService).removeAllTransformations(utr, "id")
      }
    }
  }

  ".get" should {

    "not perform auditing" when {
      "the feature toggle is set to false" in {

        val SUT = new GetTrustController(new FakeIdentifierAction(Organisation), auditService, desService, transformationService)

        val response = TrustFoundResponse(ResponseHeader("Parked", "1"))
        when(desService.getTrustInfo(any(), any())(any()))
          .thenReturn(Future.successful(response))

        when(mockConfig.auditingEnabled).thenReturn(false)

        val result = SUT.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditConnector, times(0)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "perform auditing" when {
      "the feature toggle is set to true" in {

        val SUT = new GetTrustController(new FakeIdentifierAction(Organisation), auditService, desService, transformationService)

        when(desService.getTrustInfo(any(), any())(any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        when(mockConfig.auditingEnabled).thenReturn(true)

        val result = SUT.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditConnector, times(1)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "return 200 - Ok with parked content" in {

      when(desService.getTrustInfo(any(), any())(any())).thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
      }
    }

    "return 200 - Ok with processed content" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.get(utr, applyTransformations = true).apply(FakeRequest(GET, s"/trusts/$utr"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe getTransformedApiResponse
      }
    }

    "return 400 - BadRequest" when {
      "the UTR given is invalid" in {

        val result = getTrustController.get(invalidUTR).apply(FakeRequest(GET, s"/trusts/$invalidUTR"))

        whenReady(result) { _ =>
          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "return 204 - NoContent" when {

      "the get endpoint returns a NotEnoughDataResponse" in {

        when(desService.getTrustInfo(any(), any())(any()))
          .thenReturn(Future.successful(NotEnoughDataResponse))

        val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe NO_CONTENT
        }
      }
    }

    "return 500 - InternalServerError" when {
      "the get endpoint returns a InvalidUTRResponse" in {

        when(desService.getTrustInfo(any(), any())(any()))
          .thenReturn(Future.successful(InvalidUTRResponse))

        val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$invalidUTR"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns an InvalidRegimeResponse" in {

        when(desService.getTrustInfo(any(), any())(any()))
          .thenReturn(Future.successful(InvalidRegimeResponse))

       val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a BadRequestResponse" in {

        when(desService.getTrustInfo(any(), any())(any()))
          .thenReturn(Future.successful(BadRequestResponse))

        val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a ResourceNotFoundResponse" in {

        when(desService.getTrustInfo(any(), any())(any()))
          .thenReturn(Future.successful(ResourceNotFoundResponse))

        val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe NOT_FOUND
        }
      }

      "the get endpoint returns a InternalServerErrorResponse" in {

        when(desService.getTrustInfo(any(), any())(any()))
          .thenReturn(Future.successful(InternalServerErrorResponse))

        val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the get endpoint returns a ServiceUnavailableResponse" in {

        when(desService.getTrustInfo(any(), any())(any()))
          .thenReturn(Future.successful(ServiceUnavailableResponse))

         val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  ".getLeadTrustee" should {

    "return 403 - Forbidden with parked content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.getLeadTrustee(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/lead-trustee"))

      whenReady(result) { _ =>
        status(result) mustBe FORBIDDEN
      }
    }
    "return 200 - Ok with processed content" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier])).thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getLeadTrustee(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/lead-trustee"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe getTransformedLeadTrusteeResponse
      }
    }
    "return 500 - Internal server error for invalid content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

      val result = getTrustController.getLeadTrustee(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/lead-trustee"))

      whenReady(result) { _ =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".getTrustDetails" should {
    "return 403 - Forbidden with parked content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.getTrustDetails(utr).apply(FakeRequest(GET, s"/trusts/$utr/trust-details"))

      whenReady(result) { _ =>
        status(result) mustBe FORBIDDEN
      }
    }
    "return 200 - Ok with processed content" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getTrustDetails(utr).apply(FakeRequest(GET, s"/trusts/$utr/trust-details"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.parse(
          """
            |{
            | "startDate":"1920-03-28",
            | "lawCountry":"AD",
            | "administrationCountry":"GB",
            | "residentialStatus": {
            |   "uk":{
            |     "scottishLaw":false,
            |     "preOffShore":"GB"
            |   }
            | },
            | "typeOfTrust":"Will Trust or Intestacy Trust",
            | "deedOfVariation":"Previously there was only an absolute interest under the will",
            | "interVivos":true,
            | "efrbsStartDate": "1920-02-28"
            |}""".stripMargin)
      }
    }
    "return 500 - Internal server error for invalid content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

      val result = getTrustController.getTrustDetails(utr).apply(FakeRequest(GET, s"/trusts/$utr/trust-details"))

      whenReady(result) { _ =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".getTrustees" should {

    "return 403 - Forbidden with parked content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/trustees"))

      whenReady(result) { _ =>
        status(result) mustBe FORBIDDEN
      }
    }

    "return 200 - Ok with processed content" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))
      
      val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/trustees"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe getTransformedTrusteesResponse
      }
    }

    "return 500 - Internal server error for invalid content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

      val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/trustees"))

      whenReady(result) { _ =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  ".getBeneficiaries" should {

    "return 403 - Forbidden with parked content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/beneficiaries"))

      whenReady(result) { _ =>
        status(result) mustBe FORBIDDEN
      }
    }

    "return 200 - Ok with processed content" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/beneficiaries"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe getTransformedBeneficiariesResponse
      }
    }

    "return 500 - Internal server error for invalid content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

      val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/beneficiaries"))

      whenReady(result) { _ =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  ".getSettlors" should {

    "return 403 - Forbidden with parked content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/settlors"))

      whenReady(result) { _ =>
        status(result) mustBe FORBIDDEN
      }
    }

    "return 200 - Ok with processed content" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/settlors"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe getTransformedSettlorsResponse
      }
    }

    "return 500 - Internal server error for invalid content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

      val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/settlors"))

      whenReady(result) { _ =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return 200 - Ok but empty when doesn't exist" in {

      val processedResponse = TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getSettlors(utr)(FakeRequest())

      val expected = Json.parse(
        """
          |{
          | "settlors":{
          |   "settlor":[],
          |   "settlorCompany":[]
          | }
          |}
          |""".stripMargin)

      whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe expected
      }
    }
  }

  ".getDeceasedSettlorDeathRecorded" should {

    "return 403 - Forbidden with parked content" in {

      when(desService.getTrustInfo(any(), any())(any()))
        .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

      whenReady(result) { _ =>
        status(result) mustBe FORBIDDEN
      }
    }

    "return 200 - Ok with true when exists" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(desService.getTrustInfo(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

      val expected = Json.parse("true")

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(desService).getTrustInfo(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe expected
      }
    }

    "return 500 - Internal server error for invalid content" in {

      when(desService.getTrustInfo(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

      val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

      whenReady(result) { _ =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return 200 - Ok but false when doesn't exist" in {

      val processedResponse = TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(desService.getTrustInfo(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

      val expected = Json.parse("false")

      whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(desService).getTrustInfo(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe expected
      }
    }
    "return 200 - Ok but false when exists without date" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustDeceasedSettlorWithoutDeathResponse, ResponseHeader("Processed", "1"))

      when(desService.getTrustInfo(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

      val expected = Json.parse("false")

      whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(desService).getTrustInfo(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe expected
      }
    }
  }

  ".getProtectorsAlreadyExist" should {

    "return 403 - Forbidden with parked content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

      whenReady(result) { _ =>
        status(result) mustBe FORBIDDEN
      }
    }

    "return 200 - Ok with true when exists" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

      val expected = Json.parse("true")

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe expected
      }
    }

    "return 500 - Internal server error for invalid content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

      val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

      whenReady(result) { _ =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return 200 - Ok but false when doesn't exist" in {

      val processedResponse = TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

      val expected = Json.parse("false")

      whenReady(result) { _ =>
        status(result) mustBe OK
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe expected
      }
    }
  }

  ".getProtectors" should {

    "return 403 - Forbidden with parked content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/protectors"))

      whenReady(result) { _ =>
        status(result) mustBe FORBIDDEN
      }
    }

    "return 200 - Ok with processed content" in {

      val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

      when(transformationService.getTransformedData(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(processedResponse))

      val result = getTrustController.getProtectors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/protectors"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))(any[HeaderCarrier])
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe getTransformedProtectorsResponse
      }
    }

    "return 500 - Internal server error for invalid content" in {

      when(transformationService.getTransformedData(any(), any())(any()))
        .thenReturn(Future.successful(TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

      val result = getTrustController.getProtectors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/protectors"))

      whenReady(result) { _ =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }
}

