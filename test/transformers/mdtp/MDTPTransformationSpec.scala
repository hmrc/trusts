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

package transformers.mdtp

import models.get_trust.{ResponseHeader, TrustProcessedResponse}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.JsPath
import utils.JsonUtils

class MDTPTransformationSpec extends AnyFreeSpec {

  "transforming response to mdtp" - {

    "must transform trustees" in {

      val etmpResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")
      val afterJson    = JsonUtils.getJsonValueFromFile("trust-transformed-get-api-result-after-trustee-transform.json")

      val processedResponse = TrustProcessedResponse(etmpResponse, ResponseHeader("Processed", "1"))

      val result = processedResponse.transform.get

      result.getTrust
        .transform((JsPath \ Symbol("details") \ Symbol("trust") \ Symbol("entities") \ Symbol("trustees")).json.pick)
        .get mustBe afterJson
    }

    "must transform beneficiaries" in {

      val etmpResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-one-of-each-beneficiary.json")
      val afterJson    =
        JsonUtils.getJsonValueFromFile("trust-transformed-get-api-result-after-beneficiaries-transform.json")

      val processedResponse = models.get_trust.TrustProcessedResponse(etmpResponse, ResponseHeader("Processed", "1"))

      val result = processedResponse.transform.get

      result.getTrust
        .transform(
          (JsPath \ Symbol("details") \ Symbol("trust") \ Symbol("entities") \ Symbol("beneficiary")).json.pick
        )
        .get mustBe afterJson
    }

    "must transform settlors" in {

      val etmpResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-one-of-each-protector.json")
      val afterJson    = JsonUtils.getJsonValueFromFile("trust-transformed-get-api-result-after-settlors-transform.json")

      val processedResponse = models.get_trust.TrustProcessedResponse(etmpResponse, ResponseHeader("Processed", "1"))

      val result = processedResponse.transform.get

      result.getTrust
        .transform((JsPath \ Symbol("details") \ Symbol("trust") \ Symbol("entities") \ Symbol("settlors")).json.pick)
        .get mustBe afterJson
    }

    "must transform protectors" in {

      val etmpResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-one-of-each-protector.json")
      val afterJson    = JsonUtils.getJsonValueFromFile("trust-transformed-get-api-result-after-protectors-transform.json")

      val processedResponse = models.get_trust.TrustProcessedResponse(etmpResponse, ResponseHeader("Processed", "1"))

      val result = processedResponse.transform.get

      result.getTrust
        .transform((JsPath \ Symbol("details") \ Symbol("trust") \ Symbol("entities") \ Symbol("protectors")).json.pick)
        .get mustBe afterJson
    }

  }

}
