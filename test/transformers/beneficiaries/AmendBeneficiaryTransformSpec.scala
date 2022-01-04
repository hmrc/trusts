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

import models.variation._
import models.{AddressType, NameType, PassportType}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.{JsString, JsValue, Json}
import utils.JsonUtils

import java.time.LocalDate

class AmendBeneficiaryTransformSpec extends AnyFreeSpec {

  "the amend beneficiary transformer" - {

    "when charity" - {

      val beneficiaryType: String = "charity"

      "before declaration" - {

        "amend a beneficiaries details by replacing the beneficiary" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-after.json")

          val amended = BeneficiaryCharityType(
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

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "organisationName": "Charity 2",
              |  "identification": {
              |    "utr": "0000000000"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-after-declaration.json")

          val amended = BeneficiaryCharityType(
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

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "organisationName": "Charity 2",
              |  "identification": {
              |    "utr": "0000000000"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }

        "amend the new beneficiary that is not known to etmp" in {
          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-charity-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-charity-beneficiary-transform-after-declaration.json")

          val amended = BeneficiaryCharityType(
            lineNo = None,
            bpMatchStatus = None,
            organisationName = "Amended Charity Name",
            beneficiaryDiscretion = None,
            beneficiaryShareOfIncome = None,
            identification = None,
            countryOfResidence = None,
            entityStart = LocalDate.parse("2018-02-28"),
            entityEnd = None
          )

          val original: JsValue = Json.parse(
            """
              |{
              |  "organisationName":  "Charity 3",
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(2), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }

    "when company" - {

      val beneficiaryType: String = "company"

      "before declaration" - {

        "amend a beneficiary's details by replacing the beneficiary" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-company-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-company-beneficiary-transform-after.json")

          val amended = BeneficiaryCompanyType(
            lineNo = None,
            bpMatchStatus = None,
            organisationName = "Company Name",
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

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "organisationName": "Company 2",
              |  "identification": {
              |    "utr": "0000000000"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-company-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-company-beneficiary-transform-after-declaration.json")

          val amended = BeneficiaryCompanyType(
            lineNo = None,
            bpMatchStatus = None,
            organisationName = "Updated Company Name",
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

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "organisationName": "Company 2",
              |  "identification": {
              |    "utr": "0000000000"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }

        "amend the new beneficiary that is not known to etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-company-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-company-beneficiary-transform-after-declaration.json")

          val amended = BeneficiaryCompanyType(
            lineNo = None,
            bpMatchStatus = None,
            organisationName = "Amended Company Name",
            beneficiaryDiscretion = None,
            beneficiaryShareOfIncome = None,
            identification = None,
            countryOfResidence = None,
            entityStart = LocalDate.parse("2018-02-28"),
            entityEnd = None
          )

          val original: JsValue = Json.parse(
            """
              |{
              |  "organisationName": "Company 3",
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(2), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }

    "when individual" - {

      val beneficiaryType: String = "individualDetails"

      "before declaration" - {

        "amend a beneficiaries details by replacing the beneficiary" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-beneficiary-transform-after.json")

          val amended = IndividualDetailsType(
            lineNo = None,
            bpMatchStatus = None,
            name = NameType("First 2", None, "Last 2"),
            dateOfBirth = None,
            vulnerableBeneficiary = Some(false),
            beneficiaryType = None,
            beneficiaryDiscretion = None,
            beneficiaryShareOfIncome = None,
            identification = Some(IdentificationType(
              nino = None,
              passport = Some(PassportType(
                number = "123456789",
                expirationDate = LocalDate.parse("2025-01-01"),
                countryOfIssue = "DE"
              )),
              address = None,
              safeId = None
            )),
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None,
            entityStart = LocalDate.parse("2018-02-28"),
            entityEnd = None
          )

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "name": {
              |    "firstName": "First 2",
              |    "lastName": "Last 2"
              |  },
              |  "dateOfBirth": "2010-05-03",
              |  "vulnerableBeneficiary": true,
              |  "identification": {
              |    "nino": "JP1212122A"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-beneficiary-transform-after-declaration.json")

          val amended = IndividualDetailsType(
            lineNo = None,
            bpMatchStatus = None,
            name = NameType("Updated First 2", None, "Updated Last 2"),
            dateOfBirth = Some(LocalDate.parse("2012-01-01")),
            vulnerableBeneficiary = Some(false),
            beneficiaryType = None,
            beneficiaryDiscretion = None,
            beneficiaryShareOfIncome = None,
            identification = Some(IdentificationType(
              nino = None,
              passport = Some(PassportType(
                number = "123456789",
                expirationDate = LocalDate.parse("2025-01-01"),
                countryOfIssue = "DE"
              )),
              address = None,
              safeId = None
            )),
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None,
            entityStart = LocalDate.parse("2018-02-28"),
            entityEnd = None
          )

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "name": {
              |    "firstName": "First 2",
              |    "lastName": "Last 2"
              |  },
              |  "dateOfBirth": "2010-05-03",
              |  "vulnerableBeneficiary": true,
              |  "identification": {
              |    "nino": "JP1212122A"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }

        "amend the new beneficiary that is not known to etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-individual-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-individual-beneficiary-transform-after-declaration.json")

          val amended = IndividualDetailsType(
            lineNo = None,
            bpMatchStatus = None,
            name = NameType("Amended New First 3", None, "Amended New Last 3"),
            dateOfBirth = None,
            vulnerableBeneficiary = Some(true),
            beneficiaryType = None,
            beneficiaryDiscretion = None,
            beneficiaryShareOfIncome = None,
            identification = None,
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None,
            entityStart = LocalDate.parse("2018-02-28"),
            entityEnd = None
          )

          val original: JsValue = Json.parse(
            """
              |{
              |  "name": {
              |    "firstName": "New First 3",
              |    "lastName": "New Last 3"
              |  },
              |  "vulnerableBeneficiary": false,
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(2), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }

    "when other" - {

      val beneficiaryType = "other"

      "before declaration" - {

        "amend a beneficiaries details by replacing the beneficiary" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-other-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-other-beneficiary-transform-after.json")

          val amended = OtherType(
            lineNo = None,
            bpMatchStatus = None,
            description = "Amended Description",
            address = Some(AddressType("Amended House", "Amended Street", None, None, Some("NE1 1EN"), "GB")),
            beneficiaryDiscretion = Some(false),
            beneficiaryShareOfIncome = None,
            countryOfResidence = None,
            entityStart = LocalDate.parse("2019-02-12"),
            entityEnd = None
          )

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo":"2",
              |  "address":{
              |    "line1":"House 2",
              |    "line2":"Street 2",
              |    "postCode":"NE1 1NE",
              |    "country":"GB"
              |  },
              |  "description":"Other 2",
              |  "entityStart":"2019-02-12"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-other-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-other-beneficiary-transform-after-declaration.json")

          val amended = OtherType(
            lineNo = None,
            bpMatchStatus = None,
            description = "Amended Description",
            address = Some(AddressType("Amended House", "Amended Street", None, None, Some("NE1 1EN"), "GB")),
            beneficiaryDiscretion = Some(false),
            beneficiaryShareOfIncome = None,
            countryOfResidence = None,
            entityStart = LocalDate.parse("2019-02-12"),
            entityEnd = None
          )

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo":"2",
              |  "address":{
              |    "line1":"House 2",
              |    "line2":"Street 2",
              |    "postCode":"NE1 1NE",
              |    "country":"GB"
              |  },
              |  "description":"Other 2",
              |  "entityStart":"2019-02-12"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }

        "amend the new beneficiary that is not known to etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-other-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-other-beneficiary-transform-after-declaration.json")

          val amended = OtherType(
            lineNo = None,
            bpMatchStatus = None,
            description = "Amended Description",
            address = Some(AddressType("Amended House", "Amended Street", None, None, Some("NE1 1EN"), "GB")),
            beneficiaryDiscretion = Some(false),
            beneficiaryShareOfIncome = None,
            countryOfResidence = None,
            entityStart = LocalDate.parse("2020-02-12"),
            entityEnd = None
          )

          val original: JsValue = Json.parse(
            """
              |{
              |  "description":"New 3",
              |  "entityStart":"2020-02-12"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(2), Json.toJson(amended), original, LocalDate.parse("2020-03-12"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }

    "when trust" - {

      val beneficiaryType = "trust"

      "before declaration" - {

        "amend a beneficiaries details by replacing the beneficiary" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-trust-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-trust-beneficiary-transform-after.json")

          val amended = BeneficiaryTrustType(
            lineNo = None,
            bpMatchStatus = None,
            organisationName = "Trust Name",
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

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "organisationName": "Trust 2",
              |  "identification": {
              |    "utr": "0000000000"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-trust-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-trust-beneficiary-transform-after-declaration.json")

          val amended = BeneficiaryTrustType(
            lineNo = None,
            bpMatchStatus = None,
            organisationName = "Updated Trust Name",
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

          val original: JsValue = Json.parse(
            """
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "organisationName": "Trust 2",
              |  "identification": {
              |    "utr": "0000000000"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }

        "amend the new beneficiary that is not known to etmp" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-trust-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-new-trust-beneficiary-transform-after-declaration.json")

          val amended = BeneficiaryTrustType(
            lineNo = None,
            bpMatchStatus = None,
            organisationName = "Amended Trust Name",
            beneficiaryDiscretion = None,
            beneficiaryShareOfIncome = None,
            identification = None,
            countryOfResidence = None,
            entityStart = LocalDate.parse("2018-02-28"),
            entityEnd = None
          )

          val original: JsValue = Json.parse(
            """
              |{
              |  "organisationName":  "Trust 3",
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendBeneficiaryTransform(Some(2), Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"), beneficiaryType)

          val applied = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }

    "when unidentified" - {

      val originalJson = Json.parse(
        """
          |{
          |  "lineNo":"2",
          |  "bpMatchStatus": "01",
          |  "description": "Some Description 2",
          |  "beneficiaryShareOfIncome": "25",
          |  "entityStart": "2018-02-28"
          |}
          |""".stripMargin)

      val beneficiaryType = "unidentified"

      val endDate = LocalDate.of(2012, 12, 20)

      "before declaration" - {

        "successfully update a beneficiary's details" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-after.json")
          val newDescription = "This description has been updated"
          val transformer = AmendBeneficiaryTransform(Some(1), JsString(newDescription), originalJson, endDate, beneficiaryType)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {
          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-after-declaration.json")
          val newDescription = "This description has been updated"
          val transformer = AmendBeneficiaryTransform(Some(1), JsString(newDescription), originalJson, endDate, beneficiaryType)

          val transformed = transformer.applyTransform(beforeJson).get
          val result = transformer.applyDeclarationTransform(transformed).get
          result mustBe afterJson
        }
      }
    }
  }
}
