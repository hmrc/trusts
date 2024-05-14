/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.EitherT
import config.AppConfig
import controllers.actions.{FakeIdentifierAction, ValidateIdentifierActionProvider}
import errors.{ServerError, TrustErrors}
import models.FirstTaxYearAvailable
import models.get_trust.GetTrustResponse.CLOSED_REQUEST_STATUS
import models.get_trust._
import models.taxable_migration.MigrationStatus._
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import org.scalatest.time.{Millis, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.BodyParsers
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services.auditing.AuditService
import services.{TaxYearService, TransformationService, TrustsService}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.NonTaxable5MLDFixtures.Cache.getTransformedNonTaxableTrustResponse
import utils._

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetTrustControllerSpec extends AnyWordSpec with MockitoSugar with BeforeAndAfter
  with BeforeAndAfterEach with JsonFixtures
  with Inside with ScalaFutures with GuiceOneAppPerSuite {

  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  private val trustsService: TrustsService = mock[TrustsService]
  private val mockedAuditService: AuditService = mock[AuditService]
  private val mockAuditConnector = mock[AuditConnector]
  private val mockConfig = mock[AppConfig]
  private val transformationService = mock[TransformationService]
  private val taxYearService = mock[TaxYearService]

  private val auditService = new AuditService(mockAuditConnector)

  private val validateIdentifierAction = app.injector.instanceOf[ValidateIdentifierActionProvider]
  private lazy val bodyParsers: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]

  private val mockRequiredDetailsUtil: RequiredEntityDetailsForMigration = mock[RequiredEntityDetailsForMigration]

  override def afterEach(): Unit = {
    reset(mockedAuditService)
    reset(trustsService)
    reset(mockAuditConnector)
    reset(mockConfig)
    reset(transformationService)
    reset(mockRequiredDetailsUtil)
  }

  private def getTrustController = {
    new GetTrustController(
      new FakeIdentifierAction(bodyParsers, Organisation),
      mockedAuditService,
      trustsService,
      transformationService,
      taxYearService,
      validateIdentifierAction,
      mockRequiredDetailsUtil,
      Helpers.stubControllerComponents()
    )
  }

  private val invalidUTR = "1234567"
  private val utr = "1234567890"
  private val urn = "1234567890ABCDE"
  private val invalidURN = "1234567890%£$£$"

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val sessionId: String = Session.id(hc)

  ".getFromEtmp" when {

    "utr provided" should {

      "reset the cache and clear transforms before calling get" in {

        val response = TrustFoundResponse(ResponseHeader("Parked", "1"))

        when(trustsService.resetCache(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Unit](Future.successful(Right(()))))
        when(transformationService.removeAllTransformations(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))
        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))

        val result = getTrustController.getFromEtmp(utr).apply(FakeRequest(GET, s"/trusts/$utr/refresh"))

        whenReady(result) { _ =>
          verify(trustsService).resetCache(utr, "id", sessionId)
          verify(transformationService).removeAllTransformations(utr, "id", sessionId)
        }
      }
    }

    "urn provided" should {

      "reset the cache and clear transforms before calling get" in {

        val response = TrustFoundResponse(ResponseHeader("Parked", "1"))

        when(trustsService.resetCache(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Unit](Future.successful(Right(()))))
        when(transformationService.removeAllTransformations(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))
        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))

        val result = getTrustController.getFromEtmp(urn).apply(FakeRequest(GET, s"/trusts/$urn/refresh"))

        whenReady(result) { _ =>
          verify(trustsService).resetCache(urn, "id", sessionId)
          verify(transformationService).removeAllTransformations(urn, "id", sessionId)
        }
      }

      "return 500 - InternalServerError when repository.resetCache fails" in {

        val response = TrustFoundResponse(ResponseHeader("Parked", "1"))

        when(trustsService.resetCache(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Unit](Future.successful(Left(ServerError()))))
        when(transformationService.removeAllTransformations(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))
        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))

        val result = getTrustController.getFromEtmp(urn).apply(FakeRequest(GET, s"/trusts/$urn/refresh"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

  }

  ".get" should {

    "perform auditing" in {

      val SUT = new GetTrustController(
        new FakeIdentifierAction(bodyParsers, Organisation),
        auditService,
        trustsService,
        transformationService,
        taxYearService,
        validateIdentifierAction,
        mockRequiredDetailsUtil,
        Helpers.stubControllerComponents()
      )

      when(trustsService.getTrustInfo(any(), any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

      val result = SUT.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

      whenReady(result) { _ =>
        verify(mockAuditConnector, times(1)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
      }
    }

    "return 200 - Ok with parked content" in {

      when(trustsService.getTrustInfo(any(), any(), any()))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

      val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

      whenReady(result) { _ =>
        verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
      }
    }

    "provided a UTR" must {

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(JsonFixtures.getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.get(utr, applyTransformations = true).apply(FakeRequest(GET, s"/trusts/$utr"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())

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

      "return 500 - InternalServerError" when {

        "the get endpoint returns a NotEnoughDataResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(NotEnoughDataResponse(Json.obj(), Json.obj())))))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            status(result) mustBe NO_CONTENT
          }
        }
      }

      "return 500 - InternalServerError" when {

        "the get endpoint returns a BadRequestResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(BadRequestResponse))))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }

        "the get endpoint returns a ResourceNotFoundResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(ResourceNotFoundResponse))))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe NOT_FOUND
          }
        }

        "the get endpoint returns a InternalServerErrorResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(InternalServerErrorResponse))))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }

        "the get endpoint returns a ServiceUnavailableResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(ServiceUnavailableResponse))))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe SERVICE_UNAVAILABLE
          }
        }

        "the get endpoint returns a ClosedRequestResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(ClosedRequestResponse))))

          val result = getTrustController.get(utr).apply(FakeRequest(GET, s"/trusts/$utr"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe CLOSED_REQUEST_STATUS
          }
        }
      }
    }

    "provided a URN" must {

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(NonTaxable5MLDFixtures.Cache.getTransformedNonTaxableTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.get(urn, applyTransformations = true).apply(FakeRequest(GET, s"/trusts/$urn"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(urn), mockEq("id"), mockEq(sessionId))(any())

          status(result) mustBe OK

          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe NonTaxable5MLDFixtures.Trusts.getTransformedNonTaxableTrustResponse
        }
      }

      "return 400 - BadRequest" when {

        "the URN given is invalid" in {

          val result = getTrustController.get(invalidURN).apply(FakeRequest(GET, s"/trusts/$invalidURN"))

          whenReady(result) { _ =>
            status(result) mustBe BAD_REQUEST
          }
        }
      }

      "return 500 - InternalServerError" when {

        "the get endpoint returns a NotEnoughDataResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(NotEnoughDataResponse(Json.obj(), Json.obj())))))

          val result = getTrustController.get(urn).apply(FakeRequest(GET, s"/trusts/$urn"))

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            status(result) mustBe NO_CONTENT
          }
        }
      }

      "return 500 - InternalServerError" when {

        "the get endpoint returns a BadRequestResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(BadRequestResponse))))

          val result = getTrustController.get(urn).apply(FakeRequest(GET, s"/trusts/$urn"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }

        "the get endpoint returns a ResourceNotFoundResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(ResourceNotFoundResponse))))

          val result = getTrustController.get(urn).apply(FakeRequest(GET, s"/trusts/$urn"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe NOT_FOUND
          }
        }

        "the get endpoint returns a InternalServerErrorResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(InternalServerErrorResponse))))

          val result = getTrustController.get(urn).apply(FakeRequest(GET, s"/trusts/$urn"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }

        "the get endpoint returns a ServiceUnavailableResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(ServiceUnavailableResponse))))

          val result = getTrustController.get(urn).apply(FakeRequest(GET, s"/trusts/$urn"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe SERVICE_UNAVAILABLE
          }
        }

        "the get endpoint returns a ClosedRequestResponse" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(ClosedRequestResponse))))

          val result = getTrustController.get(urn).apply(FakeRequest(GET, s"/trusts/$urn"))

          whenReady(result) { _ =>
            verify(mockedAuditService).auditErrorResponse(mockEq("GetTrust"), any[JsValue], any[String], any[String])(any())
            status(result) mustBe CLOSED_REQUEST_STATUS
          }
        }
      }

    }

    ".getLeadTrustee" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getLeadTrustee(utr).apply(FakeRequest(GET, s"/trusts/trustees/$utr/transformed/lead-trustee"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with lead trustee with 5MLD data" in {

        val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

        val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getLeadTrustee(utr).apply(FakeRequest(GET, s"/trusts/trustees/$utr/transformed/lead-trustee"))

        val expectedResponseFromTrusts = Taxable5MLDFixtures.Trusts.LeadTrustee.taxable5mld2134514321LeadTrustee

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expectedResponseFromTrusts
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

        val result = getTrustController.getLeadTrustee(utr).apply(FakeRequest(GET, s"/trusts/trustees/$utr/transformed/lead-trustee"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    ".getTrustDetails" when {

      "applying transformations" should {

        val applyTransformations = true

        "return 403 - Forbidden with parked content" in {

          when(transformationService.getTransformedData(any(), any(), any())(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

          val result = getTrustController.getTrustDetails(utr, applyTransformations).apply(FakeRequest(GET, s"/trusts/trust-details/$utr/transformed"))

          whenReady(result) { _ =>
            status(result) mustBe FORBIDDEN
          }
        }

        "return 200 - Ok with processed content" in {

          val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

          when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.getTrustDetails(utr, applyTransformations).apply(FakeRequest(GET, s"/trusts/trust-details/$utr/transformed"))

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.parse(
              """
                |{
                |  "startDate":"1920-03-28",
                |  "lawCountry":"AD",
                |  "administrationCountry":"GB",
                |  "residentialStatus": {
                |    "uk":{
                |      "scottishLaw":false,
                |      "preOffShore":"GB"
                |    }
                |  },
                |  "typeOfTrust":"Will Trust or Intestacy Trust",
                |  "deedOfVariation":"Previously there was only an absolute interest under the will",
                |  "interVivos":true,
                |  "efrbsStartDate": "1920-02-28"
                |}""".stripMargin)
          }
        }

        "return 200 - Ok with trust details with 5mld data" in {

          val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

          val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

          when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.getTrustDetails(utr, applyTransformations).apply(FakeRequest(GET, s"/trusts/trust-details/$utr/transformed"))

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.parse(
              """
                |{
                |  "startDate": "1920-03-28",
                |  "lawCountry": "AD",
                |  "administrationCountry": "GB",
                |  "residentialStatus": {
                |    "uk": {
                |      "scottishLaw": false,
                |      "preOffShore": "GB"
                |    }
                |  },
                |  "typeOfTrust": "Will Trust or Intestacy Trust",
                |  "deedOfVariation": "Previously there was only an absolute interest under the will",
                |  "interVivos": true,
                |  "efrbsStartDate": "1920-02-28",
                |  "trustTaxable": true,
                |  "expressTrust": true,
                |  "trustUKResident": true,
                |  "trustUKProperty": true,
                |  "trustRecorded": false,
                |  "trustUKRelation": false
                |}""".stripMargin)
          }
        }

        "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

          when(transformationService.getTransformedData(any(), any(), any())(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

          val result = getTrustController.getTrustDetails(utr, applyTransformations).apply(FakeRequest(GET, s"/trusts/trust-details/$utr/transformed"))

          whenReady(result) { _ =>
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }
      }

      "not applying transformations" should {

        val applyTransformations = false

        "return 403 - Forbidden with parked content" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

          val result = getTrustController.getTrustDetails(utr, applyTransformations).apply(FakeRequest(GET, s"/trusts/trust-details/$utr/transformed"))

          whenReady(result) { _ =>
            status(result) mustBe FORBIDDEN
          }
        }

        "return 200 - Ok with processed content" in {

          val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

          when(trustsService.getTrustInfo(any[String], any[String], any[String]))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.getTrustDetails(utr, applyTransformations).apply(FakeRequest(GET, s"/trusts/trust-details/$utr/transformed"))

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(trustsService).getTrustInfo(mockEq(utr), mockEq("id"), mockEq(sessionId))
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.parse(
              """
                |{
                |  "startDate":"1920-03-28",
                |  "lawCountry":"AD",
                |  "administrationCountry":"GB",
                |  "residentialStatus": {
                |    "uk":{
                |      "scottishLaw":false,
                |      "preOffShore":"GB"
                |    }
                |  },
                |  "typeOfTrust":"Will Trust or Intestacy Trust",
                |  "deedOfVariation":"Previously there was only an absolute interest under the will",
                |  "interVivos":true,
                |  "efrbsStartDate": "1920-02-28"
                |}""".stripMargin)
          }
        }

        "return 200 - Ok with trust details with 5mld data" in {

          val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

          val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

          when(trustsService.getTrustInfo(any[String], any[String], any[String]))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.getTrustDetails(utr, applyTransformations).apply(FakeRequest(GET, s"/trusts/trust-details/$utr/transformed"))

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(trustsService).getTrustInfo(mockEq(utr), mockEq("id"), mockEq(sessionId))
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.parse(
              """
                |{
                |  "startDate": "1920-03-28",
                |  "lawCountry": "AD",
                |  "administrationCountry": "GB",
                |  "residentialStatus": {
                |    "uk": {
                |      "scottishLaw": false,
                |      "preOffShore": "GB"
                |    }
                |  },
                |  "typeOfTrust": "Will Trust or Intestacy Trust",
                |  "deedOfVariation": "Previously there was only an absolute interest under the will",
                |  "interVivos": true,
                |  "efrbsStartDate": "1920-02-28",
                |  "trustTaxable": true,
                |  "expressTrust": true,
                |  "trustUKResident": true,
                |  "trustUKProperty": true,
                |  "trustRecorded": false,
                |  "trustUKRelation": false
                |}""".stripMargin)
          }
        }

        "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

          when(trustsService.getTrustInfo(any(), any(), any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError()))))

          val result = getTrustController.getTrustDetails(utr, applyTransformations).apply(FakeRequest(GET, s"/trusts/trust-details/$utr/transformed"))

          whenReady(result) { _ =>
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }

    ".getYearsReturns" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getYearsReturns(utr).apply(FakeRequest(GET, s"/trusts/tax-liability/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponseWithYearsReturns, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getYearsReturns(utr).apply(FakeRequest(GET, s"/trusts/tax-liability/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Json.parse(
            """
              |{
              |  "returns": [
              |    {
              |      "taxReturnYear": "18",
              |      "taxConsequence": true
              |    },
              |    {
              |      "taxReturnYear": "19",
              |      "taxConsequence": true
              |    },
              |    {
              |      "taxReturnYear": "20",
              |      "taxConsequence": true
              |    }
              |  ]
              |}""".stripMargin)
        }
      }

      "return 200 - Ok with years returns with 5mld data" in {

        val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

        val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getYearsReturns(utr).apply(FakeRequest(GET, s"/trusts/tax-liability/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Json.parse(
            """
              |{
              |  "returns": [
              |    {
              |      "taxReturnYear": "18",
              |      "taxConsequence": true
              |    },
              |    {
              |      "taxReturnYear": "19",
              |      "taxConsequence": true
              |    },
              |    {
              |      "taxReturnYear": "20",
              |      "taxConsequence": true
              |    }
              |  ]
              |}""".stripMargin)
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

        val result = getTrustController.getYearsReturns(utr).apply(FakeRequest(GET, s"/trusts/tax-liability/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    ".getTrustees" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/trustees/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/trustees/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedTrusteesResponse
        }
      }

      "return 200 - Ok with processed content with 5mld data" in {

        val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

        val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/trustees/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Taxable5MLDFixtures.Trusts.Trustees.taxable5mld2134514321Trustees
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError()))))

        val result = getTrustController.getTrustees(utr).apply(FakeRequest(GET, s"/trusts/trustees/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".getAssets" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getAssets(utr)(FakeRequest(GET, s"/trusts/assets/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustAllAssetsResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getAssets(utr)(FakeRequest(GET, s"/trusts/assets/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedAllAssetsResponse
        }
      }

      "return 200 - Ok with processed content with 5mld data for a taxable trust" in {

        val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

        val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getAssets(utr)(FakeRequest(GET, s"/trusts/assets/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Taxable5MLDFixtures.Trusts.Assets.taxable5mld2134514321Assets
        }
      }

      "return 200 - Ok with processed content with 5mld data for a non-taxable trust" in {

        val cached = NonTaxable5MLDFixtures.Cache.getTransformedNonTaxableTrustResponse

        val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getAssets(utr)(FakeRequest(GET, s"/trusts/assets/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe NonTaxable5MLDFixtures.Trusts.Assets.nonTaxable5mldAssets
        }
      }
      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

        val result = getTrustController.getAssets(utr)(FakeRequest(GET, s"/trusts/assets/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".getBeneficiaries" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/beneficiaries/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/beneficiaries/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedBeneficiariesResponse
        }
      }

      "return 200 - Ok with processed content with 5mld data" in {

        val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

        val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/beneficiaries/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Taxable5MLDFixtures.Trusts.Beneficiaries.taxable5mld2134514321Beneficiaries
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

        val result = getTrustController.getBeneficiaries(utr)(FakeRequest(GET, s"/trusts/beneficiaries/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".getSettlors" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/settlors/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/settlors/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedSettlorsResponse
        }
      }

      "return 200 - Ok with processed content with 5mld data" in {

        val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

        val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/settlors/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Taxable5MLDFixtures.Trusts.Settlors.taxable5mld2134514321Settlors
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

        val result = getTrustController.getSettlors(utr)(FakeRequest(GET, s"/trusts/settlors/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
      "return 200 - Ok but empty when doesn't exist" in {

        val processedResponse = TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getSettlors(utr)(FakeRequest())

        val expected = Json.parse(
          """
            |{
            |  "settlors":{
            |    "settlor":[],
            |    "settlorCompany":[]
            |  }
            |}
            |""".stripMargin)

        whenReady(result) { _ =>
          status(result) mustBe OK
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".getDeceasedSettlorDeathRecorded" should {

      "return 403 - Forbidden with parked content" in {

        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with true when exists" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(trustsService.getTrustInfo(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        val expected = Json.parse("true")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(trustsService).getTrustInfo(mockEq(utr), mockEq("id"), mockEq(sessionId))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(trustsService.getTrustInfo(any(), any(), any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError()))))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
      "return 200 - Ok but false when doesn't exist" in {

        val processedResponse = TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(trustsService.getTrustInfo(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          status(result) mustBe OK
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(trustsService).getTrustInfo(mockEq(utr), mockEq("id"), mockEq(sessionId))
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
      "return 200 - Ok but false when exists without date" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustDeceasedSettlorWithoutDeathResponse, ResponseHeader("Processed", "1"))

        when(trustsService.getTrustInfo(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getDeceasedSettlorDeathRecorded(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          status(result) mustBe OK
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(trustsService).getTrustInfo(mockEq(utr), mockEq("id"), mockEq(sessionId))
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".getProtectorsAlreadyExist" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with true when exists" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("true")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

        val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
      "return 200 - Ok but false when doesn't exist" in {

        val processedResponse = TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getProtectorsAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          status(result) mustBe OK
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".getProtectors" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getBeneficiaries(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getProtectors(utr)(FakeRequest())

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedProtectorsResponse
        }
      }

      "return 200 - Ok with processed content with 5MLD data" in {

        val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

        val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getProtectors(utr)(FakeRequest(GET, s"/trusts/protectors/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Taxable5MLDFixtures.Trusts.Protectors.taxable5mld2134514321Protectors
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

        val result = getTrustController.getProtectors(utr)(FakeRequest(GET, s"/trusts/protectors/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".getOtherIndividuals" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getOtherIndividuals(utr)(FakeRequest(GET, s"/trusts/other-individuals/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getOtherIndividuals(utr)(FakeRequest(GET, s"/trusts/other-individuals/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe getTransformedOtherIndividualsResponse
        }
      }

      "return 200 - Ok with processed content with 5mld data" in {

        val cached = Taxable5MLDFixtures.Cache.taxable5mld2134514321

        val processedResponse = TrustProcessedResponse(cached, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getOtherIndividuals(utr)(FakeRequest(GET, s"/trusts/other-individuals/$utr/transformed"))

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Taxable5MLDFixtures.Trusts.OtherIndividuals.taxable5mld2134514321OtherIndividuals
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError()))))

        val result = getTrustController.getOtherIndividuals(utr)(FakeRequest(GET, s"/trusts/other-individuals/$utr/transformed"))

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".getOtherIndividualsAlreadyExist" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(
            Right(TrustFoundResponse(ResponseHeader("Parked", "1")))
          )))

        val result = getTrustController.getOtherIndividualsAlreadyExist(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with true when exists" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getOtherIndividualsAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("true")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError()))))

        val result = getTrustController.getOtherIndividualsAlreadyExist(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
      "return 200 - Ok but false when doesn't exist" in {

        val processedResponse = TrustProcessedResponse(getEmptyTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getOtherIndividualsAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          status(result) mustBe OK
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".getNonEeaCompaniesAlreadyExist" must {

      "return true when exists" in {

        val processedResponse = TrustProcessedResponse(getTransformedNonTaxableTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getNonEeaCompaniesAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("true")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }

      "return false when does not exist" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getNonEeaCompaniesAlreadyExist(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".isTrust5mld" should {

      "return 200 - Ok with true when 5mld" in {

        val trustWith5mldData = getTransformedTrustResponse5mld

        val processedResponse = TrustProcessedResponse(trustWith5mldData, ResponseHeader("Processed", "1"))

        when(trustsService.getTrustInfo(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.isTrust5mld(utr)(FakeRequest())

        val expected = Json.parse("true")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(trustsService).getTrustInfo(mockEq(utr), mockEq("id"), mockEq(sessionId))
          verify(transformationService, never()).getTransformedData(any(), any(), any())(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }

      "return 200 - Ok with false when 4mld" in {

        val trustWith4mldData = getTransformedTrustResponse

        val processedResponse = TrustProcessedResponse(trustWith4mldData, ResponseHeader("Processed", "1"))

        when(trustsService.getTrustInfo(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.isTrust5mld(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(trustsService).getTrustInfo(mockEq(utr), mockEq("id"), mockEq(sessionId))
          verify(transformationService, never()).getTransformedData(any(), any(), any())(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".wasTrustRegisteredWithDeceasedSettlor" should {

      "return 200 - Ok with true when deceased settlor present in trust data" in {

        val trustWithDeceasedSettlor = getTransformedTrustResponse

        val processedResponse = TrustProcessedResponse(trustWithDeceasedSettlor, ResponseHeader("Processed", "1"))

        when(trustsService.getTrustInfo(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.wasTrustRegisteredWithDeceasedSettlor(utr)(FakeRequest())

        val expected = Json.parse("true")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(trustsService).getTrustInfo(mockEq(utr), mockEq("id"), mockEq(sessionId))
          verify(transformationService, never()).getTransformedData(any(), any(), any())(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }

      "return 200 - Ok with false when deceased settlor not present in trust data" in {

        val trustWithoutDeceasedSettlor = getEmptyTransformedTrustResponse

        val processedResponse = TrustProcessedResponse(trustWithoutDeceasedSettlor, ResponseHeader("Processed", "1"))

        when(trustsService.getTrustInfo(any[String], any[String], any[String]))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.wasTrustRegisteredWithDeceasedSettlor(utr)(FakeRequest())

        val expected = Json.parse("false")

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(trustsService).getTrustInfo(mockEq(utr), mockEq("id"), mockEq(sessionId))
          verify(transformationService, never()).getTransformedData(any(), any(), any())(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe expected
        }
      }
    }

    ".getTrustName" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getTrustName(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val result = getTrustController.getTrustName(utr)(FakeRequest())

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Json.toJson("Nelson James")
        }
      }

      "return 500 - Internal server error when a Left(TrustErrors) is returned" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

        val result = getTrustController.getTrustName(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }

    ".areBeneficiariesCompleteForMigration" should {

      "return Ok with true" when {
        "beneficiaries are complete for migration" in {

          when(mockRequiredDetailsUtil.areBeneficiariesCompleteForMigration(any())).thenReturn(JsSuccess(Updated))

          val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

          when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.areBeneficiariesCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.toJson(Updated)
          }
        }
      }

      "return Ok with false" when {
        "beneficiaries are not complete for migration" in {

          when(mockRequiredDetailsUtil.areBeneficiariesCompleteForMigration(any())).thenReturn(JsSuccess(NeedsUpdating))

          val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

          when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.areBeneficiariesCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.toJson(NeedsUpdating)
          }
        }
      }

      "return Ok with None" when {
        "settlors are not complete for migration" in {

          when(mockRequiredDetailsUtil.areBeneficiariesCompleteForMigration(any())).thenReturn(JsSuccess(NothingToUpdate))

          val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

          when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.areBeneficiariesCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.toJson(NothingToUpdate)
          }
        }
      }

      "return 403 (FORBIDDEN)" when {
        "parked content" in {

          when(transformationService.getTransformedData(any(), any(), any())(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

          val result = getTrustController.areBeneficiariesCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            status(result) mustBe FORBIDDEN
          }
        }
      }

      "return 500 (INTERNAL_SERVER_ERROR)" when {
        "required details util returns error" in {

          when(mockRequiredDetailsUtil.areBeneficiariesCompleteForMigration(any())).thenReturn(JsError())

          when(transformationService.getTransformedData(any(), any(), any())(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TransformationErrorResponse("")))))

          val result = getTrustController.areBeneficiariesCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }

    ".areSettlorsCompleteForMigration" should {

      "return Ok with Some(true)" when {
        "settlors are complete for migration" in {

          when(mockRequiredDetailsUtil.areSettlorsCompleteForMigration(any())).thenReturn(JsSuccess(Updated))

          val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

          when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.areSettlorsCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.toJson(Updated)
          }
        }
      }

      "return Ok with Some(false)" when {
        "settlors are not complete for migration" in {

          when(mockRequiredDetailsUtil.areSettlorsCompleteForMigration(any())).thenReturn(JsSuccess(NeedsUpdating))

          val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

          when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.areSettlorsCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.toJson(NeedsUpdating)
          }
        }
      }

      "return Ok with None" when {
        "settlors are not complete for migration" in {

          when(mockRequiredDetailsUtil.areSettlorsCompleteForMigration(any())).thenReturn(JsSuccess(NothingToUpdate))

          val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

          when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

          val result = getTrustController.areSettlorsCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
            verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
            status(result) mustBe OK
            contentType(result) mustBe Some(JSON)
            contentAsJson(result) mustBe Json.toJson(NothingToUpdate)
          }
        }
      }

      "return 403 (FORBIDDEN)" when {
        "parked content" in {

          when(transformationService.getTransformedData(any(), any(), any())(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

          val result = getTrustController.areSettlorsCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            status(result) mustBe FORBIDDEN
          }
        }
      }

      "return 500 (INTERNAL_SERVER_ERROR)" when {
        "required details util returns error" in {

          when(mockRequiredDetailsUtil.areBeneficiariesCompleteForMigration(any())).thenReturn(JsError())

          when(transformationService.getTransformedData(any(), any(), any())(any()))
            .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

          val result = getTrustController.areSettlorsCompleteForMigration(utr)(FakeRequest())

          whenReady(result) { _ =>
            status(result) mustBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }

    ".getFirstTaxYearAvailable" should {

      "return 403 - Forbidden with parked content" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(TrustFoundResponse(ResponseHeader("Parked", "1"))))))

        val result = getTrustController.getFirstTaxYearAvailable(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe FORBIDDEN
        }
      }

      "return 200 - Ok with processed content" in {

        val processedResponse = TrustProcessedResponse(getTransformedTrustResponse, ResponseHeader("Processed", "1"))

        when(transformationService.getTransformedData(any[String], any[String], any[String])(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(processedResponse))))

        val fakeResult = FirstTaxYearAvailable(1, earlierYearsToDeclare = false)

        when(taxYearService.firstTaxYearAvailable(any())).thenReturn(fakeResult)

        val result = getTrustController.getFirstTaxYearAvailable(utr)(FakeRequest())

        whenReady(result) { _ =>
          verify(mockedAuditService).audit(mockEq("GetTrust"), any[JsValue], any[String], any[JsValue])(any())
          verify(transformationService).getTransformedData(mockEq(utr), mockEq("id"), mockEq(sessionId))(any())
          verify(taxYearService).firstTaxYearAvailable(LocalDate.parse("1920-03-28"))
          status(result) mustBe OK
          contentType(result) mustBe Some(JSON)
          contentAsJson(result) mustBe Json.toJson(fakeResult)
        }
      }

      "return 500 - Internal server error when getTransformedData returns a Left(TrustErrors)" in {

        when(transformationService.getTransformedData(any(), any(), any())(any()))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("transformation error")))))

        val result = getTrustController.getFirstTaxYearAvailable(utr)(FakeRequest())

        whenReady(result) { _ =>
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

    }
  }
}
