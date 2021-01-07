/*
 * Copyright 2021 HM Revenue & Customs
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

package transformers.beneficiaries

import models.NameType
import models.variation.{IdentificationType, IndividualDetailsType}
import org.scalatest.{FreeSpec, MustMatchers}
import utils.JsonUtils

import java.time.LocalDate

class AddIndividualBeneficiaryTransformSpec extends FreeSpec with MustMatchers {

  val newBeneficiary = IndividualDetailsType(None,
    None,
    NameType("First", None, "Last"),
    Some(LocalDate.parse("2000-01-01")),
    vulnerableBeneficiary = Some(false),
    None,
    None,
    None,
    Some(IdentificationType(Some("nino"), None, None, None)),
    None,
    None,
    None,
    LocalDate.parse("1990-10-10"),
    None
  )

  "the add individual beneficiary transformer should" - {

    "add a new individual beneficiary when there are no beneficiaries existing" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")

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

  }
}