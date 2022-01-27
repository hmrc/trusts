/*
 * Copyright 2022 HM Revenue & Customs
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

import models.{AddressType, NameType}
import models.variation._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class AddBeneficiaryTransformSpec extends AnyFreeSpec {

  "the add beneficiary transformer" - {

    "when charity" - {

      val beneficiary: BeneficiaryCharityType = BeneficiaryCharityType(
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

      "should add a new beneficiary" in {
        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-charity-beneficiary.json")

        val transformer = new AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

    "when company" - {

      val beneficiary = BeneficiaryCompanyType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Organisation Name",
        beneficiaryDiscretion = Some(false),
        beneficiaryShareOfIncome = Some("50"),
        identification = Some(IdentificationOrgType(
          Some("company utr"),
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
          None)),
        countryOfResidence = None,
        entityStart = LocalDate.parse("1990-10-10"),
        entityEnd = None
      )

      val beneficiaryType: String = "company"

      "should add a new beneficiary" in {
        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-company-beneficiary.json")

        val transformer = new AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

    "when individual" - {

      val beneficiary = IndividualDetailsType(None,
        bpMatchStatus = None,
        name = NameType("First", None, "Last"),
        dateOfBirth = Some(LocalDate.parse("2000-01-01")),
        vulnerableBeneficiary = Some(false),
        beneficiaryType = None,
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        identification = Some(IdentificationType(Some("nino"), None, None, None)),
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        entityStart = LocalDate.parse("1990-10-10"),
        entityEnd = None
      )

      val beneficiaryType: String = "individualDetails"

      "should add a new beneficiary" in {
        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-individual-beneficiary.json")

        val transformer = new AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

    "when large" - {

      val beneficiary = LargeType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Name",
        description = "Description",
        description1 = None,
        description2 = None,
        description3 = None,
        description4 = None,
        numberOfBeneficiary = "501",
        identification = Some(IdentificationOrgType(
          None,
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
          None
        )),
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        countryOfResidence = None,
        entityStart = LocalDate.parse("2010-01-01"),
        entityEnd = None
      )

      val beneficiaryType: String = "large"

      "should add a new beneficiary" in {
        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-large-beneficiary.json")

        val transformer = new AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

    "when other" - {

      val beneficiary = OtherType(
        lineNo = None,
        bpMatchStatus = None,
        description = "Other",
        address = Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
        beneficiaryDiscretion = Some(false),
        beneficiaryShareOfIncome = None,
        countryOfResidence = None,
        entityStart = LocalDate.parse("1990-10-10"),
        entityEnd = None
      )

      val beneficiaryType: String = "other"

      "should add a new beneficiary" in {
        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-other-beneficiary.json")

        val transformer = new AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

    "when trust" - {

      val beneficiary = BeneficiaryTrustType(
        lineNo = None,
        bpMatchStatus = None,
        organisationName = "Organisation Name",
        beneficiaryDiscretion = Some(false),
        beneficiaryShareOfIncome = Some("50"),
        identification = Some(IdentificationOrgType(
          Some("company utr"),
          Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
          None)),
        countryOfResidence = None,
        entityStart = LocalDate.parse("1990-10-10"),
        entityEnd = None
      )

      val beneficiaryType: String = "trust"

      "should add a new beneficiary" in {
        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-trust-beneficiary.json")

        val transformer = new AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

    "when unidentified" - {

      val beneficiary = UnidentifiedType(
        lineNo = None,
        bpMatchStatus = None,
        description = "Some Description",
        beneficiaryDiscretion = None,
        beneficiaryShareOfIncome = None,
        entityStart = LocalDate.parse("1990-10-10"),
        entityEnd = None
      )

      val beneficiaryType: String = "unidentified"

      "should add a new beneficiary" in {
        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-unidentified-beneficiary.json")

        val transformer = new AddBeneficiaryTransform(Json.toJson(beneficiary), beneficiaryType)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }
  }
}