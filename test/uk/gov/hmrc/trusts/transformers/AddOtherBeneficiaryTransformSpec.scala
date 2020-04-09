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
import uk.gov.hmrc.trusts.models.variation.{BeneficiaryCharityType, IdentificationType, IndividualDetailsType, OtherType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddOtherBeneficiaryTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  val newBeneficiary = OtherType(
    None,
    None,
    "Other",
    Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
    Some(false),
    None,
    DateTime.parse("1990-10-10"),
    None
  )

  "the add other beneficiary transformer should" - {

    "add a new charity beneficiary" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-other-beneficiary.json")

      val transformer = new AddOtherBeneficiaryTransform(newBeneficiary)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}