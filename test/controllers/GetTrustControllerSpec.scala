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

package controllers

import config.AppConfig
import controllers.actions.{FakeIdentifierAction, ValidateIdentifierActionProvider}
import models.get_trust._
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services.{AuditService, DesService, TransformationService, TrustsStoreService}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{JsonFixtures, NonTaxable5MLDFixtures}

import scala.concurrent.Future

class GetTrustControllerSpec extends WordSpec with MockitoSugar
  with MustMatchers with BeforeAndAfter
  with BeforeAndAfterEach with JsonFixtures
  with Inside with ScalaFutures with GuiceOneAppPerSuite {

  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private val desService: DesService = mock[DesService]
  private val mockedAuditService: AuditService = mock[AuditService]

  private val mockAuditConnector = mock[AuditConnector]
  private val mockConfig = mock[AppConfig]

  private val transformationService = mock[TransformationService]

  private val trustsStoreService = mock[TrustsStoreService]

  private val auditService = new AuditService(mockAuditConnector, mockConfig)

  private val validateIdentififerAction = app.injector.instanceOf[ValidateIdentifierActionProvider]

  lazy val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  override def afterEach(): Unit =  {
    reset(mockedAuditService, desService, mockAuditConnector, mockConfig, transformationService)
  }

  private def getTrustController = {
    val SUT = new GetTrustController(new FakeIdentifierAction(bodyParsers, Organisation),
      mockedAuditService, desService, transformationService, validateIdentififerAction, trustsStoreService, Helpers.stubControllerComponents())

    SUT
  }

  val invalidUTR = "1234567"
  val utr = "1234567890"
  val urn = "1234567890ABCDE"

  ".getFromEtmp" when {

    "utr provided" should {

      "reset the cache and clear transforms before calling get" in {

        val SUT = new GetTrustController(new FakeIdentifierAction(bodyParsers, Organisation),
          auditService, desService, transformationService,validateIdentififerAction, trustsStoreService, Helpers.stubControllerComponents())

        val response = TrustFoundResponse(ResponseHeader("Parked", "1"))
        when(desService.resetCache(any(), any()))
          .thenReturn(Future.successful(()))

        when(transformationService.removeAllTransformations(any(), any()))
          .thenReturn(Future.successful(None))

      when(desService.getTrustInfo(any(), any()))
        .thenReturn(Future.successful(response))

        val result = SUT.getFromEtmp(utr).apply(FakeRequest(GET, s"/trusts/$utr/refresh"))

        whenReady(result) { _ =>
          verify(desService).resetCache(utr, "id")
          verify(transformationService).removeAllTransformations(utr, "id")
        }
      }
    }

    "urn provided" should {

      "reset the cache and clear transforms before calling get" in {

        val SUT = new GetTrustController(new FakeIdentifierAction(bodyParsers, Organisation),
          auditService, desService, transformationService,validateIdentififerAction, trustsStoreService, Helpers.stubControllerComponents())

        val response = TrustFoundResponse(ResponseHeader("Parked", "1"))
        when(desService.resetCache(any(), any()))
          .thenReturn(Future.successful(()))

        when(transformationService.removeAllTransformations(any(), any()))
          .thenReturn(Future.successful(None))

        when(desService.getTrustInfo(any(), any()))
          .thenReturn(Future.successful(response))

        val result = SUT.getFromEtmp(urn).apply(FakeRequest(GET, s"/trusts/$urn/refresh"))

        whenReady(result) { _ =>
          verify(desService).resetCache(urn, "id")
          verify(transformationService).removeAllTransformations(urn, "id")
        }
      }

    }

  }

  ".get" should {

    "not perform auditing" when {
      "the feature toggle is set to false" in {

        val SUT = new GetTrustController(
          new FakeIdentifierAction(bodyParsers, Organisation),
          auditService,
          desService,
          transformationService,
          validateIdentififerAction,
          trustsStoreService,
          Helpers.stubControllerComponents()
        )

        val response = TrustFoundResponse(ResponseHeader("Parked", "1"))
        when(desService.getTrustInfo(any(), any()))
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

        val SUT = new GetTrustController(
          new FakeIdentifierAction(bodyParsers, Organisation),
          auditService,
          desService,
          transformationService,
          validateIdentififerAction,
          trustsStoreService,
          Helpers.stubControllerComponents()
        )

        when(desService.getTrustInfo(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        when(mockConfig.auditingEnabled).thenReturn(true)

        val result = SUT.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockAuditConnector, times(1)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "return 200 - Ok with parked content" in {

      when(desService.getTrustInfo(any(), any())).thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

      val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
      }
    }

    "provided a UTR" must {
      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.get(utr, applyTransformations = true).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedApiResponse
        }
      }

      "provided a URN" must {

        "return 200 - Ok with processed content" in {

          val processedResponse = TrustProcessedResponse(NonTaxable5MLDFixtures.TransformCache.getTransformedNonTaxableTrustResponse, ResponseHeader("Processed", "1"))

          when(transformationService.getTransformedData(any[String], any[String]))
            .thenReturn(Future.successful(processedResponse))

          val result = getTrustController.get(urn, applyTransformations = true).apply(FakeRequest(GET, s"/trusts/$urn"))

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(transformationService).getTransformedData(mockEq(urn), mockEq("id"))
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe NonTaxable5MLDFixtures.Trusts.getTransformedNonTaxableTrustResponse
          }
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

      "return 500 - InternalServerError" when {

        "the get endpoint returns a NotEnoughDataResponse" in {

          when(desService.getTrustInfo(any(), any()))
            .thenReturn(Future.successful(NotEnoughDataResponse(Json.obj(), Json.obj())))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            status(result) mustBe NO_CONTENT
          }
        }
      }

      "return 500 - InternalServerError" when {
        "the get endpoint returns a InvalidUTRResponse" in {

          when(desService.getTrustInfo(any(), any()))
            .thenReturn(Future.successful(InvalidUTRResponse))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$invalidUTR"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }

        "the get endpoint returns an InvalidRegimeResponse" in {

          when(desService.getTrustInfo(any(), any()))
            .thenReturn(Future.successful(InvalidRegimeResponse))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }

        "the get endpoint returns a BadRequestResponse" in {

          when(desService.getTrustInfo(any(), any()))
            .thenReturn(Future.successful(BadRequestResponse))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }

        "the get endpoint returns a ResourceNotFoundResponse" in {

          when(desService.getTrustInfo(any(), any()))
            .thenReturn(Future.successful(ResourceNotFoundResponse))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe NOT_FOUND
          }
        }

        "the get endpoint returns a InternalServerErrorResponse" in {

          when(desService.getTrustInfo(any(), any()))
            .thenReturn(Future.successful(InternalServerErrorResponse))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }

        "the get endpoint returns a ServiceUnavailableResponse" in {

          when(desService.getTrustInfo(any(), any()))
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

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getLeadTrustee(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/lead-trustee"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }
      "return 200 - Ok with processed content" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String])).thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getLeadTrustee(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/lead-trustee"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedLeadTrusteeResponse
        }
      }

      "return 500 - Internal server error for invalid content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getLeadTrustee(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/lead-trustee"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    ".getTrustDetails" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getTrustDetails(utr).apply(FakeRequest(GET, s"/trusts/$utr/trust-details"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getTrustDetails(utr).apply(FakeRequest(GET, s"/trusts/$utr/trust-details"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
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

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getTrustDetails(utr).apply(FakeRequest(GET, s"/trusts/$utr/trust-details"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    ".getTrustees" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/trustees"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/trustees"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedTrusteesResponse
        }
      }

      "return 500 - Internal server error for invalid content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/$utr/transformed/trustees"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".getBeneficiaries" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/beneficiaries"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/beneficiaries"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedBeneficiariesResponse
        }
      }

      "return 500 - Internal server error for invalid content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/beneficiaries"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".getSettlors" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/settlors"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/settlors"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedSettlorsResponse
        }
      }

      "return 500 - Internal server error for invalid content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/settlors"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
      "return 200 - Ok but empty when doesn't exist" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
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
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".getDeceasedSettlorDeathRecorded" should {

      "return 403 - Forbidden with parked content" in {

        when(desService.getTrustInfo(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with true when exists" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(desService.getTrustInfo(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        val expected = Json.parse("true")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(desService).getTrustInfo(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }

      "return 500 - Internal server error for invalid content" in {

        when(desService.getTrustInfo(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
      "return 200 - Ok but false when doesn't exist" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(desService.getTrustInfo(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          status(result) mustBe OK
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(desService).getTrustInfo(mockEq(utr), mockEq("id"))
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
      "return 200 - Ok but false when exists without date" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustDeceasedSettlorWithoutDeathResponse, ResponseHeader("Processed", "1"))

        when(desService.getTrustInfo(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          status(result) mustBe OK
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(desService).getTrustInfo(mockEq(utr), mockEq("id"))
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".getProtectorsAlreadyExist" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with true when exists" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("true")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }

      "return 500 - Internal server error for invalid content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
      "return 200 - Ok but false when doesn't exist" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          status(result) mustBe OK
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".getProtectors" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/protectors"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getProtectors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/protectors"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedProtectorsResponse
        }
      }

      "return 500 - Internal server error for invalid content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getProtectors(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/protectors"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".getOtherIndividuals" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getOtherIndividuals(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/other-individuals"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getOtherIndividuals(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/other-individuals"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedOtherIndividualsResponse
        }
      }

      "return 500 - Internal server error for invalid content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getOtherIndividuals(utr)(FakeRequest(GET, s"/trusts/$utr/transformed/other-individuals"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".getOtherIndividualsAlreadyExist" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(TrustFoundResponse(ResponseHeader("Parked", "1"))))

        val result = getTrustController.getOtherIndividualsAlreadyExist(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with true when exists" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getOtherIndividualsAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("true")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }

      "return 500 - Internal server error for invalid content" in {

        when(transformationService.getTransformedData(any(), any()))
          .thenReturn(Future.successful(models.get_trust.TrustProcessedResponse(Json.obj(), ResponseHeader("Parked", "1"))))

        val result = getTrustController.getOtherIndividualsAlreadyExist(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
      "return 200 - Ok but false when doesn't exist" in {

        val processedResponse = models.get_trust.TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String]))
          .thenReturn(Future.successful(processedResponse))

        val result = getTrustController.getOtherIndividualsAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          status(result) mustBe OK
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"))
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }
  }
}
