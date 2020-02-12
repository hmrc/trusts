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

package uk.gov.hmrc.trusts.services

import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => equalTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.exceptions.EtmpCacheDataStaleException
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.models.variation.VariationResponse
import uk.gov.hmrc.trusts.models.{AddressType, Declaration, DeclarationForApi, NameType}
import uk.gov.hmrc.trusts.transformers.DeclareNoChangeTransformer
import uk.gov.hmrc.trusts.utils.JsonRequests

import scala.concurrent.Future

class VariationServiceSpec extends WordSpec with JsonRequests with MockitoSugar with ScalaFutures with MustMatchers {

  private implicit  val hc: HeaderCarrier = new HeaderCarrier
  private val formBundleNo = "001234567890"
  private val utr = "1234567890"
  private val internalId = "InternalId"
  private val fullEtmpResponseJson = getTrustResponse
  private val trustInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]
  private val transformedJson = Json.obj("field" -> "value")

  private val declaration = Declaration(
    NameType("Handy", None, "Andy"),
    AddressType("Line1", "Line2", Some("Line3"), None, Some("POSTCODE"), "GB")
  )

  val declarationForApi = DeclarationForApi(declaration, None)

  "Declare no change" should {
    "Submits data correctly when version matches" in {
      val desService = mock[DesService]
      val auditService = mock[AuditService]
      val transformer = mock[DeclareNoChangeTransformer]

      when(desService.getTrustInfoFormBundleNo(utr)).thenReturn(Future.successful(formBundleNo))

      val response = TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", formBundleNo))

      when(desService.getTrustInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]())).thenReturn(Future.successful(
        response
      ))

      when(desService.trustVariation(any())(any[HeaderCarrier])).thenReturn(Future.successful(
        VariationResponse("TVN34567890")
      ))

      when(transformer.transform(any(),any(),any(),any())).thenReturn(JsSuccess(transformedJson))

      val OUT = new VariationService(desService, transformer, auditService)

      whenReady(OUT.submitDeclareNoChange(utr, internalId, declarationForApi)) {variationResponse => {
        variationResponse mustBe VariationResponse("TVN34567890")
        verify(transformer, times(1)).transform(equalTo(response), equalTo(response.getTrust), equalTo(declarationForApi), any())
        val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
        verify(desService, times(1)).trustVariation(arg.capture())(any[HeaderCarrier])
        arg.getValue mustBe transformedJson
      }}
    }
  }
  "Fail if the etmp data version doesn't match our submission data" in {
    val desService = mock[DesService]
    val auditService = mock[AuditService]
    val transformer = mock[DeclareNoChangeTransformer]

    when(desService.getTrustInfoFormBundleNo(utr)).thenReturn(Future.successful("31415900000"))

    when(desService.getTrustInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]())).thenReturn(Future.successful(
      TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", formBundleNo))
    ))

    when(desService.trustVariation(any())(any[HeaderCarrier])).thenReturn(Future.successful(
      VariationResponse("TVN34567890")
    ))

    when(transformer.transform(any(),any(),any(),any())).thenReturn(JsSuccess(transformedJson))

    val OUT = new VariationService(desService, transformer, auditService)

    whenReady(OUT.submitDeclareNoChange(utr, internalId, declarationForApi).failed) {exception => {
      exception mustBe an[EtmpCacheDataStaleException.type]
      verify(desService, times(0)).trustVariation(any())(any[HeaderCarrier])
    }
    }
  }
}
