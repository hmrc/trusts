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

import models.variation.{BeneficiaryCharityType, IdentificationOrgType, UnidentifiedType}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsString, Json}
import utils.JsonUtils

import java.time.LocalDate

class AmendBeneficiaryTransformSpec extends FreeSpec with MustMatchers {

  private val originalBeneficiary: BeneficiaryCharityType = BeneficiaryCharityType(
    lineNo = Some("2"),
    bpMatchStatus = Some("01"),
    organisationName = "Charity 2",
    beneficiaryDiscretion = None,
    beneficiaryShareOfIncome = None,
    identification = Some(IdentificationOrgType(
      utr = Some("0000000000"),
      address = None,
      safeId = None
    )),
    countryOfResidence = None,
    entityStart = LocalDate.parse("2018-02-28"),
    entityEnd = None
  )

  val beneficiaryType: String = "charity"

  private val endDate = LocalDate.parse("2020-03-25")

  "the amend beneficiary transformer should" - {

    "before declaration" - {

      "when unidentified beneficiary" - {

        val originalBeneficiary: UnidentifiedType = UnidentifiedType(
          lineNo = Some("2"),
          bpMatchStatus = Some("01"),
          description = "Some Description 2",
          beneficiaryDiscretion = None,
          beneficiaryShareOfIncome = Some("25"),
          entityStart = LocalDate.parse("2018-02-28"),
          entityEnd = None
        )

        val beneficiaryType: String = "unidentified"

        "must deep merge description with original data" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-after.json")

          val newDescription = "This description has been updated"
          val transformer = AmendBeneficiaryTransform(1, JsString(newDescription), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "when not unidentified beneficiary" - {

        val amendedBeneficiary: BeneficiaryCharityType = BeneficiaryCharityType(
          lineNo = None,
          bpMatchStatus = None,
          organisationName = "Charity Name",
          beneficiaryDiscretion = None,
          beneficiaryShareOfIncome = None,
          identification = Some(IdentificationOrgType(
            utr = Some("1234567890"),
            address = None,
            safeId = None
          )),
          countryOfResidence = None,
          entityStart = LocalDate.parse("2018-02-28"),
          entityEnd = None
        )

        "successfully update a beneficiary's details" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-after.json")

          val transformer = AmendBeneficiaryTransform(1, Json.toJson(amendedBeneficiary), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }

    "at declaration time" - {

      val amendedBeneficiary: BeneficiaryCharityType = BeneficiaryCharityType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Updated Charity Name",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = Some(IdentificationOrgType(
          utr = Some("1234567890"),
          address = None,
          safeId = None
        )),
        countryOfResidence = None,
        entityStart = LocalDate.parse("2018-02-28"),
        entityEnd = None
      )

      "set an end date for the original asset and add in the amendment as a new asset not known to ETMP" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-after-declaration.json")

        val transformer = AmendBeneficiaryTransform(1, Json.toJson(amendedBeneficiary), Json.toJson(originalBeneficiary), endDate, beneficiaryType)

        val transformed = transformer.applyTransform(beforeJson).get

        val result = transformer.applyDeclarationTransform(transformed).get
        result mustBe afterJson
      }
    }
  }
}
