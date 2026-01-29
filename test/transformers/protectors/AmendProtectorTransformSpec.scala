/*
 * Copyright 2024 HM Revenue & Customs
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

package transformers.protectors

import models.variation.{IdentificationOrgType, IdentificationType, ProtectorCompany, ProtectorIndividual}
import models.{NameType, PassportType}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.{JsValue, Json}
import utils.JsonUtils

import java.time.LocalDate

class AmendProtectorTransformSpec extends AnyFreeSpec {

  "the amend protector transformer" - {

    "when individual" - {

      val `type` = "protector"

      "before declaration" - {

        "amend a protector details by replacing the protector" in {

          val beforeJson =
            JsonUtils.getJsonValueFromFile("transforms/trusts-individual-protector-transform-before.json")
          val afterJson  = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-protector-transform-after.json")

          val amended = ProtectorIndividual(
            lineNo = None,
            bpMatchStatus = None,
            NameType("First 2", None, "Last 2"),
            None,
            identification = Some(
              IdentificationType(
                nino = None,
                passport = Some(
                  PassportType(
                    number = "123456789",
                    expirationDate = LocalDate.parse("2025-01-01"),
                    countryOfIssue = "DE"
                  )
                ),
                address = None,
                safeId = None
              )
            ),
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None,
            LocalDate.parse("2018-02-28"),
            None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "name": {
              |    "firstName": "First 2",
              |    "lastName": "Last 2"
              |  },
              |  "dateOfBirth": "2010-05-03",
              |  "identification": {
              |    "nino": "JP1212122A"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer =
            AmendProtectorTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), `type`)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original protector, adding in the amendment as a new protector for a protector known by etmp" in {

          val beforeJson =
            JsonUtils.getJsonValueFromFile("transforms/trusts-individual-protector-transform-before.json")

          val afterJson =
            JsonUtils.getJsonValueFromFile("transforms/trusts-individual-protector-transform-after-declaration.json")

          val amended = ProtectorIndividual(
            lineNo = None,
            bpMatchStatus = None,
            NameType("Updated First 2", None, "Updated Last 2"),
            dateOfBirth = Some(LocalDate.parse("2012-01-01")),
            identification = Some(
              IdentificationType(
                nino = None,
                passport = Some(
                  PassportType(
                    number = "123456789",
                    expirationDate = LocalDate.parse("2025-01-01"),
                    countryOfIssue = "DE"
                  )
                ),
                address = None,
                safeId = None
              )
            ),
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None,
            LocalDate.parse("2018-02-28"),
            None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "name": {
              |    "firstName": "First 2",
              |    "lastName": "Last 2"
              |  },
              |  "dateOfBirth": "2010-05-03",
              |  "identification": {
              |    "nino": "JP1212122A"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendProtectorTransform(
            Some(1),
            Json.toJson(amended),
            original,
            endDate = LocalDate.parse("2020-03-25"),
            `type`
          )

          val applied = transformer.applyTransform(beforeJson).get
          val result  = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }

        "amend the new protector that is not known to etmp" in {
          val beforeJson =
            JsonUtils.getJsonValueFromFile("transforms/trusts-new-individual-protector-transform-before.json")

          val afterJson =
            JsonUtils.getJsonValueFromFile(
              "transforms/trusts-new-individual-protector-transform-after-declaration.json"
            )

          val amended = ProtectorIndividual(
            lineNo = None,
            bpMatchStatus = None,
            NameType("Amended New First 3", None, "Amended New Last 3"),
            dateOfBirth = None,
            identification = None,
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None,
            LocalDate.parse("2018-02-28"),
            None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "name": {
              |    "firstName": "New First 3",
              |    "lastName": "New Last 3"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendProtectorTransform(
            Some(2),
            Json.toJson(amended),
            original,
            endDate = LocalDate.parse("2020-03-25"),
            `type`
          )

          val applied = transformer.applyTransform(beforeJson).get
          val result  = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }

    "when business" - {

      val `type` = "protectorCompany"

      "before declaration" - {

        "amend a protector's details by replacing the protector" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("trusts-business-protector-transform-before.json")
          val afterJson  = JsonUtils.getJsonValueFromFile("trusts-business-protector-transform-after.json")

          val amended = ProtectorCompany(
            lineNo = None,
            bpMatchStatus = None,
            "Company Name",
            identification = Some(
              IdentificationOrgType(
                utr = Some("1234567890"),
                address = None,
                safeId = None
              )
            ),
            countryOfResidence = None,
            LocalDate.parse("2018-02-28"),
            None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "name": "Company 2",
              |  "identification": {
              |    "utr": "0000000000"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer =
            AmendProtectorTransform(Some(1), Json.toJson(amended), original, LocalDate.parse("2020-03-25"), `type`)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "at declaration time" - {

        "set an end date for the original protector, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

          val beforeJson =
            JsonUtils.getJsonValueFromFile("trusts-business-protector-transform-before.json")

          val afterJson =
            JsonUtils.getJsonValueFromFile("trusts-business-protector-transform-after-declaration.json")

          val amended = ProtectorCompany(
            lineNo = None,
            bpMatchStatus = None,
            "Updated Company Name",
            identification = Some(
              IdentificationOrgType(
                utr = Some("1234567890"),
                address = None,
                safeId = None
              )
            ),
            countryOfResidence = None,
            LocalDate.parse("2018-02-28"),
            None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "lineNo": "2",
              |  "bpMatchStatus": "01",
              |  "name": "Company 2",
              |  "identification": {
              |    "utr": "0000000000"
              |  },
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendProtectorTransform(
            Some(1),
            Json.toJson(amended),
            original,
            endDate = LocalDate.parse("2020-03-25"),
            `type`
          )

          val applied = transformer.applyTransform(beforeJson).get
          val result  = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }

        "amend the new protector that is not known to etmp" in {
          val beforeJson =
            JsonUtils.getJsonValueFromFile("trusts-new-business-protector-transform-before.json")

          val afterJson =
            JsonUtils.getJsonValueFromFile("trusts-new-business-protector-transform-after-declaration.json")

          val amended = ProtectorCompany(
            lineNo = None,
            bpMatchStatus = None,
            "Amended Company Name",
            None,
            countryOfResidence = None,
            LocalDate.parse("2018-02-28"),
            None
          )

          val original: JsValue = Json.parse("""
              |{
              |  "name": "Company 3",
              |  "entityStart": "2018-02-28"
              |}
              |""".stripMargin)

          val transformer = AmendProtectorTransform(
            Some(2),
            Json.toJson(amended),
            original,
            endDate = LocalDate.parse("2020-03-25"),
            `type`
          )

          val applied = transformer.applyTransform(beforeJson).get
          val result  = transformer.applyDeclarationTransform(applied).get
          result mustBe afterJson
        }
      }
    }
  }

}
