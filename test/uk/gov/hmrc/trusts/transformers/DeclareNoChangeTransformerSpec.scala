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

package uk.gov.hmrc.trusts.transformers

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{GetTrustSuccessResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.models.{AddressType, Declaration, NameType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class DeclareNoChangeTransformerSpec extends FreeSpec with MustMatchers with OptionValues {
  "the no change transformer should" - {

    val declaration = Declaration(NameType("First", None, "Last"), AddressType("Line1", "Line2", Some("Line3"), None, Some("POSTCODE"), "GB"))

    "transform json successfully for an org lead trustee" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-sent.json")
      val transformer = new DeclareNoChangeTransformer

      val result = transformer.transform(trustResponse, declaration)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully for an individual lead trustee" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-etmp-received-individual.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-individual.json")
      val transformer = new DeclareNoChangeTransformer

      val result = transformer.transform(trustResponse, declaration)
      result.asOpt.value mustBe afterJson
    }
  }
}