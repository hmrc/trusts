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

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{GetTrustSuccessResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.models.{AddressType, AgentDetails, Declaration, DeclarationForApi, NameType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class DeclarationTransformerSpec extends FreeSpec with MustMatchers with OptionValues {
  val entityEnd = new DateTime(2020, 1, 30, 15, 0)

  "the no change transformer should" - {

    val declaration = Declaration(NameType("First", None, "Last"), AddressType("Line1", "Line2", Some("Line3"), None, Some("POSTCODE"), "GB"))
    val declarationForApi = DeclarationForApi(declaration, None)

    "transform json successfully for an org lead trustee" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-sent.json")
      val transformer = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, entityEnd)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully for an individual lead trustee" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-etmp-received-individual.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-individual.json")
      val transformer = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, entityEnd)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully for a lead trustee when the trustee has changed" in {
      val originalJson = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")
      val originalResponse = originalJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-etmp-received-individual.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-individual-with-prev-org.json")
      val transformer = new DeclarationTransformer

      val result = transformer.transform(trustResponse, originalResponse.getTrust, declarationForApi, entityEnd)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully for an individual lead trustee with agent details" in {
      val agentDetails = AgentDetails(
        "arn",
        "agent name",
        AddressType("Line1", "Line2", Some("Line3"), None, Some("POSTCODE"), "GB"),
        "01234567890",
        "client-ref"
      )

      val declarationForApi = DeclarationForApi(declaration, Some(agentDetails))
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-etmp-received-individual.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-individual-with-agent-details.json")
      val transformer = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, entityEnd)
      result.asOpt.value mustBe afterJson
    }
  }
}