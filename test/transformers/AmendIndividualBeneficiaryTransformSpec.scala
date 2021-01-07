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

package transformers

import java.time.LocalDate
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsValue, Json}
import models.variation.{IdentificationType, IndividualDetailsType}
import models.{NameType, PassportType}
import transformers.beneficiaries.AmendIndividualBeneficiaryTransform
import utils.JsonUtils

class AmendIndividualBeneficiaryTransformSpec extends FreeSpec with MustMatchers {

  "AmendIndividualBeneficiaryTransform should" - {

    "before declaration" - {

      "amend a beneficiaries details by replacing the beneficiary" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-beneficiary-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-individual-beneficiary-transform-after.json")

        val amended = IndividualDetailsType(
          lineNo = None,
          bpMatchStatus = None,
          NameType("First 2", None, "Last 2"),
          None,
          vulnerableBeneficiary = Some(false),
          None,
          None,
          None,
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
          None,
          None,
          None,
          LocalDate.parse("2018-02-28"),
          None
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

        val transformer = AmendIndividualBeneficiaryTransform(1, Json.toJson(amended), original, LocalDate.parse("2020-03-25"))

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

        val beforeJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-individual-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-individual-beneficiary-transform-after-declaration.json")

        val amended = IndividualDetailsType(
          lineNo = None,
          bpMatchStatus = None,
          NameType("Updated First 2", None, "Updated Last 2"),
          dateOfBirth = Some(LocalDate.parse("2012-01-01")),
          vulnerableBeneficiary = Some(false),
          None,
          None,
          None,
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
          None,
          None,
          None,
          LocalDate.parse("2018-02-28"),
          None
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

        val transformer = AmendIndividualBeneficiaryTransform(1, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }

      "amend the new beneficiary that is not known to etmp" in {
        val beforeJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-new-individual-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-new-individual-beneficiary-transform-after-declaration.json")

        val amended = IndividualDetailsType(
          lineNo = None,
          bpMatchStatus = None,
          NameType("Amended New First 3", None, "Amended New Last 3"),
          dateOfBirth = None,
          vulnerableBeneficiary = Some(true),
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          LocalDate.parse("2018-02-28"),
          None
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

        val transformer = AmendIndividualBeneficiaryTransform(2, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }
    }
  }
}
