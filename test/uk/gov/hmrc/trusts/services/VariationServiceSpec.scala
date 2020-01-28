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
import org.scalatest.{ MustMatchers, WordSpec}
import play.api.libs.json.JsValue
import org.mockito.Mockito.{times, verify, when}
import org.mockito.Matchers.{any, eq => equalTo}
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{GetTrustSuccessResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.models.variation.VariationResponse
import uk.gov.hmrc.trusts.models.{AddressType, Declaration, NameType}
import uk.gov.hmrc.trusts.utils.JsonRequests

import scala.concurrent.Future

class VariationServiceSpec extends WordSpec with JsonRequests with MockitoSugar with ScalaFutures with MustMatchers {

  "Declare no change" should {
//    "Fail if the form bundle number has changed" ignore {}
//    "Gets Authoritative info direct from DES" ignore {}
//    "Gets Info from Cache with deltas applied" in {
//      val utr = "1234567890"
//      val internalId = "InternalId"
//      val fullEtmpResponseJson = getTrustResponse
//      val trustInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]
//      val desService = mock[DesService]
//
//      when(desService.getTrustInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]())).thenReturn(Future.successful(
//        TrustProcessedResponse(trustInfoJson, ResponseHeader("Processed", "1234567890"))
//      ))
//
//      when(desService.trustVariation(equalTo(fullEtmpResponseJson))(any[HeaderCarrier])).thenReturn(Future.successful(
//        VariationResponse("TVN34567890")
//      ))
//
//      val declaration = Declaration(
//        NameType("Handy", None, "Andy"),
//        AddressType("Line1", "Line2", Some("Line3"), None, Some("POSTCODE"), "GB")
//      )
//
//      val OUT = new VariationService()
//
//      whenReady(OUT.submitDeclareNoChange(utr, declaration)) {variationResponse => {
//        variationResponse mustBe VariationResponse("TVN34567890")
//        val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])
//        verify(desService.trustVariation(arg.capture())(any[HeaderCarrier]), times(1))
//        arg.getValue() mustBe fullEtmpResponseJson
//      }}
//    }
//
//    "Transforms the data as needed" ignore {
//
//    }
  }
}
