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

package uk.gov.hmrc.trusts.transformers.mdtp

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.JsPath
import uk.gov.hmrc.trusts.models.get_trust_or_estate.ResponseHeader
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.utils.JsonUtils

class MDTPTransformationSpec extends FreeSpec with MustMatchers {

  "transforming response to mdtp" - {

    "must transform trustees" in {

      val etmpResponse = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trust-transformed-get-api-result-after-trustee-transform.json")

      val processedResponse = TrustProcessedResponse(etmpResponse, ResponseHeader("Processed", "1"))

      val result = processedResponse.transform.get

      result.getTrust.transform((JsPath \ 'details \ 'trust \ 'entities \ 'trustees).json.pick).get mustBe afterJson
    }

  }

}
