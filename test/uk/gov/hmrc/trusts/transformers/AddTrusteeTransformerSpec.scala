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
import play.api.libs.json.Json
import uk.gov.hmrc.time.DateTimeUtils
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{GetTrustSuccessResponse, TrustProcessedResponse}
import uk.gov.hmrc.trusts.models.variation.{TrusteeIndividualType, TrusteeOrgType, TrusteeType}
import uk.gov.hmrc.trusts.models.{AddressType, Declaration, NameType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddTrusteeTransformerSpec extends FreeSpec with MustMatchers with OptionValues {
  "the add trustee transformer should" - {

    "add a new individual trustee" in {

      val newTrustee = TrusteeType(Some(TrusteeIndividualType(None, None, NameType("FirstName", None, "Surname"), None, None, None, DateTime.parse("2020-01-30"), None)), None)

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-add-ind-trustee.json")

      val transformer = new AddTrusteeTransformer(newTrustee)

      val result = transformer.applyTransform(trustJson)

      result mustBe afterJson
    }

    "add a new organisation trustee" in {

      val newTrustee = TrusteeType(
        None,
        Some(TrusteeOrgType(None, None, "Company Name", None, None, None, DateTime.parse("2020-01-30"), None))
      )

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-add-org-trustee.json")

      val transformer = new AddTrusteeTransformer(newTrustee)

      val result = transformer.applyTransform(trustJson)

      result mustBe afterJson
    }
  }
}