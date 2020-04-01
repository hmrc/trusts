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
import uk.gov.hmrc.trusts.models.{AddressType, IdentificationOrgType, NameType}
import uk.gov.hmrc.trusts.models.variation.{CharityType, IdentificationType, IndividualDetailsType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddCharityBeneficiaryTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  val newBeneficiary = CharityType(
    None,
    None,
    "Charity",
    Some(false),
    Some("50"),
    Some(IdentificationOrgType(None, Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")))),
    DateTime.parse("1990-10-10"),
    None
  )

  "the add charity beneficiary transformer should" - {

    "add a new charity beneficiary" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-benfeciary.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-charity-beneficiary.json")

      val transformer = new AddCharityBeneficiaryTransform(newBeneficiary)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "fail to add a new charity beneficiary when there are 25 or more existing beneficiaries" in {

      val json = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-max-charity-beneficiary.json")

      val transformer = new AddCharityBeneficiaryTransform(newBeneficiary)

      val thrown = intercept[Exception] (transformer.applyTransform(json).get)

      thrown.getMessage mustBe "Adding an item to /details/trust/entities/beneficiary/charity would exceed the maximum allowed amount of 25"

    }
  }
}