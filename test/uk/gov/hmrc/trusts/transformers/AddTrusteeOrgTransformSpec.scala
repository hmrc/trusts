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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationOrgType, DisplayTrustTrusteeOrgType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddTrusteeOrgTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  "the add trustee transformer should" - {

    "add a new org trustee" in {

      val trustee = DisplayTrustTrusteeOrgType(None,
        None,
        "Company Name",
        None,
        None,
        None,
        DateTime.parse("2020-01-30"))

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-add-org-trustee.json")

      val transformer = AddTrusteeOrgTransform(trustee, alreadyAdded = 0)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "fail to add a new org trustee when there are 25 or more existing trustees" in {

      val trustee = DisplayTrustTrusteeOrgType(None,
        None,
        "New Trustee",
        Some("phoneNumber"),
        None,
        Some(DisplayTrustIdentificationOrgType(None, Some("utr"), None)),
        DateTime.parse("1990-10-10"))

      val json = JsonUtils.getJsonValueFromFile("trusts-etmp-max-trustees.json")

      val transformer = AddTrusteeOrgTransform(trustee, alreadyAdded = 0)

      val thrown = intercept[Exception] (transformer.applyTransform(json).get)

      thrown.getMessage mustBe "Adding a trustee would exceed the maximum allowed amount of 25"

    }

    "fail to add a new individual trustee when there are 25 or more existing trustees or existing AddTrustee transformers" in {

      val trustee = DisplayTrustTrusteeOrgType(None,
        None,
        "New Trustee",
        Some("phoneNumber"),
        None,
        Some(DisplayTrustIdentificationOrgType(None, Some("utr"), None)),
        DateTime.parse("1990-10-10"))

      val json = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val transformer = new AddTrusteeOrgTransform(trustee, alreadyAdded = 24)

      val thrown = intercept[Exception] (transformer.applyTransform(json).get)

      thrown.getMessage mustBe "Adding a trustee would exceed the maximum allowed amount of 25"
    }
  }
}