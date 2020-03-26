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
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.variation.{IdentificationType, IndividualDetailsType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddIndividualBeneficiaryTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  val newBeneficiary = IndividualDetailsType(None,
    None,
    NameType("First", None, "Last"),
    Some(DateTime.parse("2000-01-01")),
    false,
    None,
    None,
    None,
    Some(IdentificationType(Some("nino"), None, None, None)),
    DateTime.parse("1990-10-10"),
    None
  )

  "the add individual beneficiary transformer should" - {

    "add a new individual beneficiary when there are no beneficiaries existing" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-benfeciary.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-individual-beneficiary.json")

      val transformer = new AddIndividualBeneficiaryTransform(newBeneficiary)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new individual beneficiary" in {

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-individual-beneficiary.json")

      val transformer = new AddIndividualBeneficiaryTransform(newBeneficiary)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "fail to add a new individual beneficiary when there are 25 or more existing beneficiaries" in {

      val json = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-max-individual-beneficiary.json")

      val transformer = new AddIndividualBeneficiaryTransform(newBeneficiary)

      val thrown = intercept[Exception] (transformer.applyTransform(json).get)

      thrown.getMessage mustBe "Adding an individualDetails beneficiary would exceed the maximum allowed amount of 25"

    }
  }
}