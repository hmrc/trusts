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

package services

import exceptions.EtmpCacheDataStaleException
import models.get_trust.{ResponseHeader, TrustProcessedResponse}
import models.tax_enrolments.TaxEnrolmentSuccess
import models.variation.VariationResponse
import models.{DeclarationName, NameType}
import models.variation.DeclarationForApi
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import transformers.DeclarationTransformer
import uk.gov.hmrc.http.HeaderCarrier
import utils.{JsonFixtures, JsonUtils, NonTaxable5MLDFixtures}

import java.time.LocalDate
import scala.concurrent.Future

class VariationServiceSpec extends WordSpec
  with JsonFixtures with MockitoSugar
  with ScalaFutures with MustMatchers
  with GuiceOneAppPerSuite with BeforeAndAfterEach {

  private val auditService = mock[AuditService]

  private implicit  val hc: HeaderCarrier = new HeaderCarrier
  private val formBundleNo = "001234567890"
  private val utr = "1234567890"
  private val internalId = "InternalId"
  private val subscriberId = "TVN34567890"
  private val fullEtmpResponseJson = get4MLDTrustResponse
  private val fullEtmpResponseJson5MLD = JsonUtils.getJsonValueFromString(NonTaxable5MLDFixtures.DES.get5MLDTrustNonTaxableResponse)
  private val transformedEtmpResponseJson = Json.parse("""{ "field": "Arbitrary transformed JSON" }""")
  private val trustInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]
  private val trustInfoJson5MLD = (fullEtmpResponseJson5MLD \ "trustOrEstateDisplay").as[JsValue]
  private val transformedJson = Json.obj("field" -> "value")
  private val declaration = DeclarationName(
    NameType("Handy", None, "Andy")
  )

  private def trustsStoreService(is5mldEnabled: Boolean): TrustsStoreService = {
    val service = mock[TrustsStoreService]
    when(service.is5mldEnabled()(any(), any())).thenReturn(Future.successful(is5mldEnabled))
    service
  }
  private def trustsStoreServiceFor4mld = trustsStoreService(is5mldEnabled = false)
  private def trustsStoreServiceFor5mld = trustsStoreService(is5mldEnabled = true)

  object LocalDateServiceStub extends LocalDateService {
    override def now: LocalDate = LocalDate.of(1999, 3, 14)
  }

  val declarationForApi: DeclarationForApi = DeclarationForApi(declaration, None, None)

  override def beforeEach(): Unit = {
    reset(auditService)
  }

  "Declare no change" should {

    "submit data correctly when the version matches, and then reset the cache" in {

      val trustsService = mock[TrustsService]

      val transformationService = mock[TransformationService]
      val auditService = app.injector.instanceOf[FakeAuditService]
      val transformer = mock[DeclarationTransformer]
      val taxableMigrationService = mock[TaxableMigrationService]

      when(transformationService.populateLeadTrusteeAddress(any[JsValue])(any())).thenReturn(JsSuccess(trustInfoJson))
      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))
      when(trustsService.getTrustInfoFormBundleNo(utr)).thenReturn(Future.successful(formBundleNo))

      val response = TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", formBundleNo))

      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId))).thenReturn(Future.successful(response))

      when(trustsService.trustVariation(any())).thenReturn(Future.successful(VariationResponse(subscriberId)))

      when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId)).thenReturn(Future.successful(false))

      when(transformer.transform(any(),any(),any(),any(),any())).thenReturn(JsSuccess(transformedJson))

      val OUT = new VariationService(trustsService,
        transformationService,
        transformer,
        auditService,
        LocalDateServiceStub,
        trustsStoreServiceFor4mld,
        taxableMigrationService
      )

      val transformedResponse = TrustProcessedResponse(transformedEtmpResponseJson, ResponseHeader("Processed", formBundleNo))

      whenReady(OUT.submitDeclaration(utr, internalId, declarationForApi)) { variationResponse => {
        variationResponse mustBe VariationResponse(subscriberId)
        verify(transformationService, times( 1)).applyDeclarationTransformations(equalTo(utr), equalTo(internalId), equalTo(trustInfoJson))(any[HeaderCarrier])
        verify(transformer, times(1)).transform(equalTo(transformedResponse), equalTo(response.getTrust), equalTo(declarationForApi), any(), equalTo(false))
        val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
        verify(trustsService, times(1)).trustVariation(arg.capture())
        arg.getValue mustBe transformedJson
      }}
    }
  }

  "passes 5mld=true to the transformer when the feature is set" in {

    val trustsService = mock[TrustsService]

    val transformationService = mock[TransformationService]
    val auditService = app.injector.instanceOf[FakeAuditService]
    val transformer = mock[DeclarationTransformer]
    val taxableMigrationService = mock[TaxableMigrationService]

    when(transformationService.populateLeadTrusteeAddress(any[JsValue])(any())).thenReturn(JsSuccess(trustInfoJson5MLD))
    when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))
    when(trustsService.getTrustInfoFormBundleNo(utr)).thenReturn(Future.successful(formBundleNo))

    val response = TrustProcessedResponse(trustInfoJson5MLD, ResponseHeader("Processed", formBundleNo))

    when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId))).thenReturn(Future.successful(response))

    when(trustsService.trustVariation(any())).thenReturn(Future.successful(VariationResponse(subscriberId)))

    when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId)).thenReturn(Future.successful(false))

    when(transformer.transform(any(),any(),any(),any(),any())).thenReturn(JsSuccess(transformedJson))

    val OUT = new VariationService(trustsService,
      transformationService,
      transformer,
      auditService,
      LocalDateServiceStub,
      trustsStoreServiceFor5mld,
      taxableMigrationService
    )

    val transformedResponse = TrustProcessedResponse(transformedEtmpResponseJson, ResponseHeader("Processed", formBundleNo))

    whenReady(OUT.submitDeclaration(utr, internalId, declarationForApi)) { variationResponse => {
      variationResponse mustBe VariationResponse(subscriberId)
      verify(transformationService, times( 1)).
        applyDeclarationTransformations(equalTo(utr), equalTo(internalId), equalTo(trustInfoJson5MLD))(any[HeaderCarrier])
      verify(transformer, times(1)).transform(equalTo(transformedResponse), equalTo(response.getTrust), equalTo(declarationForApi), any(), equalTo(true))
      val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
      verify(trustsService, times(1)).trustVariation(arg.capture())
      arg.getValue mustBe transformedJson
    }}
  }

  "Fail if the etmp data version doesn't match our submission data" in {
    val trustsService = mock[TrustsService]
    val transformationService = mock[TransformationService]
    val transformer = mock[DeclarationTransformer]
    val migrationService = mock[TaxableMigrationService]

    when(trustsService.getTrustInfoFormBundleNo(utr))
      .thenReturn(Future.successful("31415900000"))

    when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))

    when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId)))
      .thenReturn(Future.successful(TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", formBundleNo))))

    when(trustsService.trustVariation(any()))
      .thenReturn(Future.successful(VariationResponse(subscriberId)))

    when(transformer.transform(any(),any(),any(),any(), any())).thenReturn(JsSuccess(transformedJson))

    val OUT = new VariationService(trustsService,
      transformationService,
      transformer,
      auditService,
      LocalDateServiceStub,
      trustsStoreServiceFor4mld,
      migrationService)

    whenReady(OUT.submitDeclaration(utr, internalId, declarationForApi).failed) { exception => {
      exception mustBe an[EtmpCacheDataStaleException.type]

      verify(trustsService, times(0)).trustVariation(any())

    }}
  }

  "auditing" should {

    val trustsService = mock[TrustsService]
    val transformationService = mock[TransformationService]
    val transformer = mock[DeclarationTransformer]
    val migrationService = mock[TaxableMigrationService]

    when(trustsService.getTrustInfoFormBundleNo(utr))
      .thenReturn(Future.successful(formBundleNo))

    val OUT = new VariationService(
      trustsService,
      transformationService,
      transformer,
      auditService,
      LocalDateServiceStub,
      trustsStoreServiceFor5mld,
      migrationService
    )

    "capture variation success when not migrating" in {

      val trustsService = mock[TrustsService]
      val transformationService = mock[TransformationService]
      val transformer = mock[DeclarationTransformer]
      val taxableMigrationService = mock[TaxableMigrationService]

      val response = TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", formBundleNo))

      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))

      when(transformationService.populateLeadTrusteeAddress(any[JsValue])(any()))
        .thenReturn(JsSuccess(trustInfoJson))

      when(trustsService.getTrustInfoFormBundleNo(utr))
        .thenReturn(Future.successful(formBundleNo))

      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId)))
        .thenReturn(Future.successful(response))

      when(trustsService.trustVariation(any()))
        .thenReturn(Future.successful(VariationResponse(subscriberId)))

      when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId)).thenReturn(Future.successful(false))

      when(transformer.transform(any(),any(),any(),any(),any()))
        .thenReturn(JsSuccess(transformedJson))

      val OUT = new VariationService(trustsService,
        transformationService,
        transformer,
        auditService,
        LocalDateServiceStub,
        trustsStoreServiceFor5mld,
        taxableMigrationService)

      whenReady(OUT.submitDeclaration(utr, internalId, declarationForApi)) { _ => {

        verify(auditService).auditVariationSubmitted(
          equalTo(internalId),
          equalTo(false),
          equalTo(transformedJson),
          equalTo(VariationResponse(subscriberId))
        )(any())

      }}
    }

    "capture variation success when migrating" in {

      val trustsService = mock[TrustsService]
      val transformationService = mock[TransformationService]
      val transformer = mock[DeclarationTransformer]
      val taxableMigrationService = mock[TaxableMigrationService]

      val response = TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", formBundleNo))

      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))

      when(transformationService.populateLeadTrusteeAddress(any[JsValue])(any()))
        .thenReturn(JsSuccess(trustInfoJson))

      when(trustsService.getTrustInfoFormBundleNo(utr))
        .thenReturn(Future.successful(formBundleNo))

      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId)))
        .thenReturn(Future.successful(response))

      when(trustsService.trustVariation(any()))
        .thenReturn(Future.successful(VariationResponse(subscriberId)))

      when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId)).thenReturn(Future.successful(true))
      when(taxableMigrationService.migrateSubscriberToTaxable(equalTo(subscriberId), equalTo(utr))(any[HeaderCarrier]))
        .thenReturn(Future.successful(TaxEnrolmentSuccess))

      when(transformer.transform(any(),any(),any(),any(),any()))
        .thenReturn(JsSuccess(transformedJson))

      val OUT = new VariationService(trustsService,
        transformationService,
        transformer,
        auditService,
        LocalDateServiceStub,
        trustsStoreServiceFor5mld,
        taxableMigrationService)

      whenReady(OUT.submitDeclaration(utr, internalId, declarationForApi)) { _ => {

        verify(auditService).auditVariationSubmitted(
          equalTo(internalId),
          equalTo(true),
          equalTo(transformedJson),
          equalTo(VariationResponse(subscriberId))
        )(any())

      }}
    }

    "capture failure to transform" in {

      val response = TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", formBundleNo))

      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(JsError("Errors")))

      when(transformationService.populateLeadTrusteeAddress(any[JsValue])(any()))
        .thenReturn(JsSuccess(trustInfoJson))

      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId)))
        .thenReturn(Future.successful(response))

      whenReady(OUT.submitDeclaration(utr, internalId, declarationForApi).failed) { _ => {

        verify(auditService).auditVariationTransformationError(
          equalTo(internalId),
          equalTo(utr),
          equalTo(trustInfoJson),
          any(),
          equalTo("Failed to apply declaration transformations."),
          any()
        )(any())

      }}
    }

    "capture failure to populate lead trustee" in {

      val response = TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", formBundleNo))

      when(transformationService.populateLeadTrusteeAddress(any[JsValue])(any()))
        .thenReturn(JsError("Error"))

      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId)))
        .thenReturn(Future.successful(response))

      whenReady(OUT.submitDeclaration(utr, internalId, declarationForApi).failed) { _ => {

        verify(auditService).auditVariationTransformationError(
          equalTo(internalId),
          equalTo(utr),
          any(),
          any(),
          equalTo("Failed to populate lead trustee address"),
          any()
        )(any())

      }}
    }

  }

  "migrateToTaxable" should {

    "performs the migration if the transformation requires it" in {

      val trustsService = mock[TrustsService]

      val transformationService = mock[TransformationService]
      val auditService = app.injector.instanceOf[FakeAuditService]
      val transformer = mock[DeclarationTransformer]
      val taxableMigrationService = mock[TaxableMigrationService]

      when(transformationService.populateLeadTrusteeAddress(any[JsValue])(any())).thenReturn(JsSuccess(trustInfoJson5MLD))
      when(transformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))
      when(trustsService.getTrustInfoFormBundleNo(utr)).thenReturn(Future.successful(formBundleNo))

      val response = TrustProcessedResponse(trustInfoJson5MLD, ResponseHeader("Processed", formBundleNo))

      when(trustsService.getTrustInfo(equalTo(utr), equalTo(internalId))).thenReturn(Future.successful(response))

      when(trustsService.trustVariation(any())).thenReturn(Future.successful(VariationResponse(subscriberId)))
      when(taxableMigrationService.migratingFromNonTaxableToTaxable(utr, internalId)).thenReturn(Future.successful(true))
      when(taxableMigrationService.migrateSubscriberToTaxable(equalTo(subscriberId), equalTo(utr))(any[HeaderCarrier]))
        .thenReturn(Future.successful(TaxEnrolmentSuccess))

      when(transformer.transform(any(),any(),any(),any(),any())).thenReturn(JsSuccess(transformedJson))

      val OUT = new VariationService(trustsService,
        transformationService,
        transformer,
        auditService,
        LocalDateServiceStub,
        trustsStoreServiceFor5mld,
        taxableMigrationService
      )

      val transformedResponse = TrustProcessedResponse(transformedEtmpResponseJson, ResponseHeader("Processed", formBundleNo))

      whenReady(OUT.submitDeclaration(utr, internalId, declarationForApi)) { variationResponse => {
        variationResponse mustBe VariationResponse(subscriberId)
        verify(transformationService, times( 1)).
          applyDeclarationTransformations(equalTo(utr), equalTo(internalId), equalTo(trustInfoJson5MLD))(any[HeaderCarrier])
        verify(transformer, times(1)).transform(equalTo(transformedResponse), equalTo(response.getTrust), equalTo(declarationForApi), any(), equalTo(true))
        val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
        verify(trustsService, times(1)).trustVariation(arg.capture())
        arg.getValue mustBe transformedJson
      }}
    }
  }
}
