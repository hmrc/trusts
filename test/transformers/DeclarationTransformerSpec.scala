/*
 * Copyright 2024 HM Revenue & Customs
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

package transformers

import models._
import models.get_trust.{GetTrustSuccessResponse, TrustProcessedResponse}
import models.variation.DeclarationForApi
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import utils.JsonUtils

import java.time.LocalDate

class DeclarationTransformerSpec extends AnyFreeSpec with OptionValues {

  private val submissionDate = LocalDate.of(2020, 1, 30)

  "the declaration transformer should" - {

    val declaration       = DeclarationName(NameType("First", None, "Last"))
    val declarationForApi = DeclarationForApi(declaration, None, None)

    "transform json successfully for an org lead trustee" in {
      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent.json")
      val transformer   = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully for an individual lead trustee" in {
      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-received-individual.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-individual.json")
      val transformer   = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully for a lead trustee when the trustee has changed" in {
      val originalJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")
      val originalResponse = originalJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]

      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-transformed-individual.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]

      val afterJson   = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-individual-with-prev-org.json")
      val transformer = new DeclarationTransformer

      val result = transformer.transform(trustResponse, originalResponse.getTrust, declarationForApi, submissionDate)
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

      val declarationForApi = DeclarationForApi(declaration, Some(agentDetails), None)
      val beforeJson        = JsonUtils.getJsonValueFromFile("trusts-etmp-received-individual.json")
      val trustResponse     = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson         = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-individual-with-agent-details.json")
      val transformer       = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully when no trustees" in {
      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-received-no-trustees.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-pruned-trustees.json")
      val transformer   = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

    "remove trustees field if trustees list is empty" in {
      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-received-empty-trustees.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-pruned-trustees.json")
      val transformer   = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully when closing trust" in {
      val declarationForApi = DeclarationForApi(declaration, None, Some(LocalDate.parse("2019-02-03")))

      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-received.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-end-date.json")
      val transformer   = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

    "add a submission date if 5mld and not received one at playback" in {
      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-received-with-years-returns.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-5mld.json")
      val transformer   = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

    "override the submission date if 5mld and received one at playback" in {
      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-received-with-submission-date.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-5mld.json")
      val transformer   = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

    "amend share assets" - {
      "transform json successfully to remove the extra fields from the share assets" in {
        val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-transformed-with-share-assets-extra-fields.json")
        val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
        val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-with-share-assets-extra-fields-removed.json")
        val transformer   = new DeclarationTransformer

        val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
        result.asOpt.value \\ "shares" mustBe afterJson \\ "shares"
        result.asOpt.value             mustBe afterJson
      }

      "ensure the transform doesn't break if we have no share assets but are doing a transform" in {
        val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-received-with-no-share-assets.json")
        val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
        val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-with-no-share-assets.json")
        val transformer   = new DeclarationTransformer

        val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
        result.asOpt.value \\ "shares" mustBe afterJson \\ "shares"
        result.asOpt.value             mustBe afterJson
      }
    }

    "amend trust details" - {
      "transform json successfully to remove extra field from trust details" in {
        val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-transformed-with-settlors-uk-based.json")
        val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
        val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-with-settlors-uk-based-removed.json")
        val transformer   = new DeclarationTransformer

        val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
        result.asOpt.value mustBe afterJson
      }
    }

    "remove detailsType fields" in {
      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-transformed-with-details-type.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-with-details-type-removed.json")
      val transformer   = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

    "transform json successfully when invalid trust details" in {
      val beforeJson    = JsonUtils.getJsonValueFromFile("trusts-etmp-invalid-trust-details.json")
      val trustResponse = beforeJson.as[GetTrustSuccessResponse].asInstanceOf[TrustProcessedResponse]
      val afterJson     = JsonUtils.getJsonValueFromFile("trusts-etmp-sent-valid-trust-details.json")
      val transformer   = new DeclarationTransformer

      val result = transformer.transform(trustResponse, trustResponse.getTrust, declarationForApi, submissionDate)
      result.asOpt.value mustBe afterJson
    }

  }

}
