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

package services

import cats.data.EitherT
import errors.{EtmpCacheDataStaleErrorResponse, InternalServerErrorResponse, ServerError, TrustErrors, VariationFailureForAudit}
import models.get_trust.{GetTrustResponse, ResourceNotFoundResponse, ResponseHeader, TrustProcessedResponse}
import models.tax_enrolments.{TaxEnrolmentSubscriberResponse, TaxEnrolmentSuccess}
import models.variation.{DeclarationForApi, VariationContext, VariationSuccessResponse}
import models.{DeclarationName, NameType}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import services.auditing.VariationAuditService
import services.dates.LocalDateService
import transformers.DeclarationTransformer
import uk.gov.hmrc.http.HeaderCarrier
import utils.{JsonFixtures, JsonUtils, NonTaxable5MLDFixtures}

import java.time.LocalDate
import java.time.Month.MARCH
import scala.concurrent.Future

class VariationServiceSpec extends AnyWordSpec
  with JsonFixtures with MockitoSugar
  with ScalaFutures with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = new HeaderCarrier
  private val formBundleNo = "001234567890"
  private val utr = "1234567890"
  private val internalId = "InternalId"
  private val sessionId: String = "sessionId"
  private val subscriberId = "TVN34567890"

  private val fullEtmpResponseJson5MLD = JsonUtils.getJsonValueFromString(NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponse)
  private val transformedEtmpResponseJson = Json.parse("""{ "field": "Arbitrary transformed JSON" }""")

  private val trustInfoJson5MLD = (fullEtmpResponseJson5MLD \ "trustOrEstateDisplay").as[JsValue]
  private val transformedJson = Json.obj("field" -> "value")
  private val declaration = DeclarationName(NameType("Handy", None, "Andy"))
  val declarationForApi: DeclarationForApi = DeclarationForApi(declaration, None, None)

  object LocalDateServiceStub extends LocalDateService {
    private val (year1999, num14) = (1999, 14)
    override def now: LocalDate = LocalDate.of(year1999, MARCH, num14)
  }

  private val auditService = mock[VariationAuditService]
  private val trustsService = mock[TrustsService]
  private val transformationService = mock[TransformationService]
  private val transformer = mock[DeclarationTransformer]
  private val taxableMigrationService = mock[TaxableMigrationService]

  override def beforeEach(): Unit = {
    reset(auditService)
    reset(trustsService)
    reset(transformationService)
    reset(transformer)
    reset(taxableMigrationService)
  }

  val application: Application = new GuiceApplicationBuilder()
    .overrides(bind[VariationAuditService].toInstance(auditService))
    .overrides(bind[TrustsService].toInstance(trustsService))
    .overrides(bind[TransformationService].toInstance(transformationService))
    .overrides(bind[DeclarationTransformer].toInstance(transformer))
    .overrides(bind[TaxableMigrationService].toInstance(taxableMigrationService))
    .build

  "Declare no change" should {
    "submit data correctly when the version matches, and then reset the cache" in {

      when(transformationService.populateLeadTrusteeAddress(any[JsValue]))
        .thenReturn(JsSuccess(trustInfoJson5MLD))
      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(EitherT[Future, TrustErrors, JsResult[JsValue]](Future.successful(Right(JsSuccess(transformedEtmpResponseJson)))))
      when(trustsService.getTrustInfoFormBundleNo(utr))
        .thenReturn(EitherT[Future, TrustErrors, String](Future.successful(Right(formBundleNo))))

      val response = TrustProcessedResponse(trustInfoJson5MLD, ResponseHeader("Processed", formBundleNo))

      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId), equalTo(sessionId)))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))
      when(trustsService.trustVariation(any()))
        .thenReturn(EitherT[Future, TrustErrors, VariationSuccessResponse](Future.successful(Right(VariationSuccessResponse(subscriberId)))))
      when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId, sessionId))
        .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(false))))
      when(transformer.transform(any(), any(), any(), any()))
        .thenReturn(JsSuccess(transformedJson))

      val OUT = application.injector.instanceOf[VariationService]

      val transformedResponse = TrustProcessedResponse(transformedEtmpResponseJson, ResponseHeader("Processed", formBundleNo))

      whenReady(OUT.submitDeclaration(utr, internalId, sessionId, declarationForApi).value) { variationResponse =>
        variationResponse mustBe Right(VariationContext(transformedJson, VariationSuccessResponse(subscriberId)))

        verify(transformationService, times(1)).
          applyDeclarationTransformations(equalTo(utr), equalTo(internalId), equalTo(trustInfoJson5MLD))(any[HeaderCarrier])
        verify(transformer, times(1)).transform(equalTo(transformedResponse), equalTo(response.getTrust), equalTo(declarationForApi), any())

        val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])

        verify(trustsService, times(1)).trustVariation(arg.capture())
        arg.getValue mustBe transformedJson
      }
    }

    "Fail if the etmp data version doesn't match our submission data" in {

      when(trustsService.getTrustInfoFormBundleNo(utr))
        .thenReturn(EitherT[Future, TrustErrors, String](Future.successful(Right("31415900000"))))
      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(EitherT[Future, TrustErrors, JsResult[JsValue]](Future.successful(Right(JsSuccess(transformedEtmpResponseJson)))))
      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId), equalTo(sessionId)))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(
          Right(TrustProcessedResponse(trustInfoJson5MLD, ResponseHeader("Processed", formBundleNo)))
        )))
      when(trustsService.trustVariation(any()))
        .thenReturn(EitherT[Future, TrustErrors, VariationSuccessResponse](Future.successful(Right(VariationSuccessResponse(subscriberId)))))
      when(transformer.transform(any(), any(), any(), any()))
        .thenReturn(JsSuccess(transformedJson))

      val OUT = application.injector.instanceOf[VariationService]

      whenReady(OUT.submitDeclaration(utr, internalId, sessionId, declarationForApi).value) { result =>
        result mustBe Left(VariationFailureForAudit(EtmpCacheDataStaleErrorResponse, "Etmp data is stale"))

        verify(trustsService, times(0)).trustVariation(any())
      }
    }

    "Fail if trust response is not a trust processed response" in {
      val trustResponse = ResourceNotFoundResponse

      when(trustsService.getTrustInfoFormBundleNo(utr))
        .thenReturn(EitherT[Future, TrustErrors, String](Future.successful(Right("31415900000"))))
      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(EitherT[Future, TrustErrors, JsResult[JsValue]](Future.successful(Right(JsSuccess(transformedEtmpResponseJson)))))
      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId), equalTo(sessionId)))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(trustResponse))))
      when(trustsService.trustVariation(any()))
        .thenReturn(EitherT[Future, TrustErrors, VariationSuccessResponse](Future.successful(Right(VariationSuccessResponse(subscriberId)))))
      when(transformer.transform(any(), any(), any(), any()))
        .thenReturn(JsSuccess(transformedJson))

      val OUT = application.injector.instanceOf[VariationService]

      whenReady(OUT.submitDeclaration(utr, internalId, sessionId, declarationForApi).value) { result =>
        result mustBe Left(VariationFailureForAudit(
          InternalServerErrorResponse,
          "Submission could not proceed, Trust data was not in a processed state"
        ))

        verify(trustsService, times(0)).trustVariation(any())
      }
    }

    "return a VariationFailureForAudit when getTrustInfo returns a ServerError(message), where message is nonEmpty" in {

      when(transformationService.populateLeadTrusteeAddress(any[JsValue]))
        .thenReturn(JsSuccess(trustInfoJson5MLD))
      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(EitherT[Future, TrustErrors, JsResult[JsValue]](Future.successful(Right(JsSuccess(transformedEtmpResponseJson)))))
      when(trustsService.getTrustInfoFormBundleNo(utr))
        .thenReturn(EitherT[Future, TrustErrors, String](Future.successful(Right(formBundleNo))))

      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId), equalTo(sessionId)))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Left(ServerError("exception errors")))))
      when(trustsService.trustVariation(any()))
        .thenReturn(EitherT[Future, TrustErrors, VariationSuccessResponse](Future.successful(Right(VariationSuccessResponse(subscriberId)))))
      when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId, sessionId))
        .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(false))))

      val OUT = application.injector.instanceOf[VariationService]

      whenReady(OUT.submitDeclaration(utr, internalId, sessionId, declarationForApi).value) { result =>
        result mustBe Left(VariationFailureForAudit(errors.InternalServerErrorResponse, "exception errors"))

      }
    }

  }

  "auditing" should {

    "capture variation success" when {
      "not migrating" in {

        val response = TrustProcessedResponse(trustInfoJson5MLD, ResponseHeader("Processed", formBundleNo))

        when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(EitherT[Future, TrustErrors, JsResult[JsValue]](Future.successful(Right(JsSuccess(transformedEtmpResponseJson)))))
        when(transformationService.populateLeadTrusteeAddress(any[JsValue]))
          .thenReturn(JsSuccess(trustInfoJson5MLD))
        when(trustsService.getTrustInfoFormBundleNo(utr))
          .thenReturn(EitherT[Future, TrustErrors, String](Future.successful(Right(formBundleNo))))
        when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId), equalTo(sessionId)))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))
        when(trustsService.trustVariation(any()))
          .thenReturn(EitherT[Future, TrustErrors, VariationSuccessResponse](Future.successful(Right(VariationSuccessResponse(subscriberId)))))
        when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId, sessionId))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(false))))
        when(transformer.transform(any(), any(), any(), any()))
          .thenReturn(JsSuccess(transformedJson))

        val OUT = application.injector.instanceOf[VariationService]

        whenReady(OUT.submitDeclaration(utr, internalId, sessionId, declarationForApi).value) { _ =>

          verify(auditService).auditVariationSuccess(
            equalTo(internalId),
            equalTo(false),
            equalTo(transformedJson),
            equalTo(VariationSuccessResponse(subscriberId))
          )(any())
        }
      }

      "migrating" in {

        val response = TrustProcessedResponse(trustInfoJson5MLD, ResponseHeader("Processed", formBundleNo))

        when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(EitherT[Future, TrustErrors, JsResult[JsValue]](Future.successful(Right(JsSuccess(transformedEtmpResponseJson)))))
        when(transformationService.populateLeadTrusteeAddress(any[JsValue]))
          .thenReturn(JsSuccess(trustInfoJson5MLD))
        when(trustsService.getTrustInfoFormBundleNo(utr))
          .thenReturn(EitherT[Future, TrustErrors, String](Future.successful(Right(formBundleNo))))
        when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId), equalTo(sessionId)))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))
        when(trustsService.trustVariation(any()))
          .thenReturn(EitherT[Future, TrustErrors, VariationSuccessResponse](Future.successful(Right(VariationSuccessResponse(subscriberId)))))
        when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId, sessionId))
          .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))
        when(taxableMigrationService.migrateSubscriberToTaxable(equalTo(subscriberId), equalTo(utr))(any[HeaderCarrier]))
          .thenReturn(EitherT[Future, TrustErrors, TaxEnrolmentSubscriberResponse](Future.successful(Right(TaxEnrolmentSuccess))))
        when(transformer.transform(any(), any(), any(), any()))
          .thenReturn(JsSuccess(transformedJson))

        val OUT = application.injector.instanceOf[VariationService]

        whenReady(OUT.submitDeclaration(utr, internalId, sessionId, declarationForApi).value) { _ =>

          verify(auditService).auditVariationSuccess(
            equalTo(internalId),
            equalTo(true),
            equalTo(transformedJson),
            equalTo(VariationSuccessResponse(subscriberId))
          )(any())
        }
      }
    }

    "capture failure" when {
      "transforming" in {

        val response = TrustProcessedResponse(trustInfoJson5MLD, ResponseHeader("Processed", formBundleNo))

        when(trustsService.getTrustInfoFormBundleNo(utr))
          .thenReturn(EitherT[Future, TrustErrors, String](Future.successful(Right(formBundleNo))))
        when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(EitherT[Future, TrustErrors, JsResult[JsValue]](Future.successful(Right(JsError("Errors")))))
        when(transformationService.populateLeadTrusteeAddress(any[JsValue]))
          .thenReturn(JsSuccess(trustInfoJson5MLD))
        when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId), equalTo(sessionId)))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))

        val OUT = application.injector.instanceOf[VariationService]

        whenReady(OUT.submitDeclaration(utr, internalId, sessionId, declarationForApi).value) { result =>

          result mustBe Left(VariationFailureForAudit(
            errors.InternalServerErrorResponse,
            "There was a problem transforming data for submission to ETMP"
          ))

          verify(auditService).auditTransformationError(
            equalTo(internalId),
            equalTo(utr),
            equalTo(trustInfoJson5MLD),
            any(),
            equalTo("Failed to apply declaration transformations."),
            any()
          )(any())
        }
      }

      "populating lead trustee" in {

        val response = TrustProcessedResponse(trustInfoJson5MLD, ResponseHeader("Processed", formBundleNo))

        when(trustsService.getTrustInfoFormBundleNo(utr))
          .thenReturn(EitherT[Future, TrustErrors, String](Future.successful(Right(formBundleNo))))
        when(transformationService.populateLeadTrusteeAddress(any[JsValue]))
          .thenReturn(JsError("Error"))
        when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId), equalTo(sessionId)))
          .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))

        val OUT = application.injector.instanceOf[VariationService]

        whenReady(OUT.submitDeclaration(utr, internalId, sessionId, declarationForApi).value) { result =>

          result mustBe Left(VariationFailureForAudit(
            errors.InternalServerErrorResponse,
            "There was a problem transforming data for submission to ETMP"
          ))

          verify(auditService).auditTransformationError(
            equalTo(internalId),
            equalTo(utr),
            any(),
            any(),
            equalTo("Failed to populate lead trustee address"),
            any()
          )(any())
        }
      }
    }
  }

  "migrateToTaxable" should {

    "performs the migration if the transformation requires it" in {

      when(transformationService.populateLeadTrusteeAddress(any[JsValue]))
        .thenReturn(JsSuccess(trustInfoJson5MLD))
      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(EitherT[Future, TrustErrors, JsResult[JsValue]](Future.successful(Right(JsSuccess(transformedEtmpResponseJson)))))
      when(trustsService.getTrustInfoFormBundleNo(utr))
        .thenReturn(EitherT[Future, TrustErrors, String](Future.successful(Right(formBundleNo))))

      val response = TrustProcessedResponse(trustInfoJson5MLD, ResponseHeader("Processed", formBundleNo))

      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId), equalTo(sessionId)))
        .thenReturn(EitherT[Future, TrustErrors, GetTrustResponse](Future.successful(Right(response))))
      when(trustsService.trustVariation(any()))
        .thenReturn(EitherT[Future, TrustErrors, VariationSuccessResponse](Future.successful(Right(VariationSuccessResponse(subscriberId)))))
      when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId, sessionId))
        .thenReturn(EitherT[Future, TrustErrors, Boolean](Future.successful(Right(true))))
      when(taxableMigrationService.migrateSubscriberToTaxable(equalTo(subscriberId), equalTo(utr))(any[HeaderCarrier]))
        .thenReturn(EitherT[Future, TrustErrors, TaxEnrolmentSubscriberResponse](Future.successful(Right(TaxEnrolmentSuccess))))
      when(transformer.transform(any(), any(), any(), any()))
        .thenReturn(JsSuccess(transformedJson))

      val OUT = application.injector.instanceOf[VariationService]

      val transformedResponse = TrustProcessedResponse(transformedEtmpResponseJson, ResponseHeader("Processed", formBundleNo))

      whenReady(OUT.submitDeclaration(utr, internalId, sessionId, declarationForApi).value) { variationContext =>
        variationContext mustBe Right(VariationContext(transformedJson, VariationSuccessResponse(subscriberId)))

        verify(transformationService, times(1)).
          applyDeclarationTransformations(equalTo(utr), equalTo(internalId), equalTo(trustInfoJson5MLD))(any[HeaderCarrier])
        verify(transformer, times(1)).transform(equalTo(transformedResponse), equalTo(response.getTrust), equalTo(declarationForApi), any())

        val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])

        verify(trustsService, times(1)).trustVariation(arg.capture())
        arg.getValue mustBe transformedJson
      }
    }
  }
}
