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

import models.AddressType
import models.variation.{BeneficiaryCharityType, IdentificationOrgType}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class AddBeneficiaryTransformSpec extends FreeSpec with MustMatchers {

  private val beneficiary: BeneficiaryCharityType = BeneficiaryCharityType(
    lineNo = None,
    bpMatchStatus = None,
    organisationName = "Charity",
    beneficiaryDiscretion = Some(false),
    beneficiaryShareOfIncome = Some("50"),
    identification = Some(IdentificationOrgType(None, Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")), None)),
    countryOfResidence = None,
    entityStart = LocalDate.parse("1990-10-10"),
    entityEnd = None
  )

  val beneficiaryType: String = "charity"

  "the add beneficiary transformer should" - {

    "add a new beneficiary" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-charity-beneficiary.json")

      val transformer = new AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}