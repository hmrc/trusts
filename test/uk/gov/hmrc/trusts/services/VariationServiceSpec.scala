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

import org.mockito.ArgumentCaptor
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsSuccess, JsValue, Json}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.Matchers.{any, eq => equalTo}
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{GetTrustSuccessResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.models.variation.VariationResponse
import uk.gov.hmrc.trusts.models.{AddressType, Declaration, NameType}
import uk.gov.hmrc.trusts.transformers.DeclareNoChangeTransformer
import uk.gov.hmrc.trusts.utils.JsonRequests

import scala.concurrent.Future

class VariationServiceSpec extends WordSpec with JsonRequests with MockitoSugar with ScalaFutures with MustMatchers {

  implicit  val hc: HeaderCarrier = new HeaderCarrier

  "Declare no change" should {
//    "Fail if the form bundle number has changed" ignore {}
//    "Gets Authoritative info direct from DES" ignore {}
    "Gets Info from Cache with deltas applied" in {
      val utr = "1234567890"
      val internalId = "InternalId"
      val fullEtmpResponseJson = getTrustResponse
      val trustInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]
      val desService = mock[DesService]
      val auditService = mock[AuditService]
      val transformer = mock[DeclareNoChangeTransformer]

      when(desService.getTrustInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]())).thenReturn(Future.successful(
        TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1234567890"))
      ))

      when(desService.trustVariation(any())(any[HeaderCarrier])).thenReturn(Future.successful(
        VariationResponse("TVN34567890")
      ))

      val transformedJson = Json.obj("field" -> "value")
      when(transformer.transform(any(),any())).thenReturn(JsSuccess(transformedJson))

      val declaration = Declaration(
        NameType("Handy", None, "Andy"),
        AddressType("Line1", "Line2", Some("Line3"), None, Some("POSTCODE"), "GB")
      )

      val OUT = new VariationService(desService, transformer, auditService)

      whenReady(OUT.submitDeclareNoChange(utr, internalId, declaration)) {variationResponse => {
        variationResponse mustBe VariationResponse("TVN34567890")
        verify(transformer, times(1)).transform(trustInfoJson, declaration)
        val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
        verify(desService, times(1)).trustVariation(arg.capture())(any[HeaderCarrier])
        arg.getValue mustBe transformedJson
      }}
    }
  }
}
