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
import models.variation.{BeneficiaryCharityType, IdentificationOrgType}
import utils.JsonUtils

class AmendCharityBeneficiaryTransformSpec extends FreeSpec with MustMatchers {

  "AmendCharityBeneficiaryTransform should" - {

    "before declaration" - {

      "amend a beneficiaries details by replacing the beneficiary" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-after.json")

        val amended = BeneficiaryCharityType(
          lineNo = None,
          bpMatchStatus = None,
          "Charity Name",
          None,
          None,
          identification = Some(IdentificationOrgType(
            utr = Some("1234567890"),
            address = None,
            safeId = None
          )),
          None,
          LocalDate.parse("2018-02-28"),
          None
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

        val transformer = AmendCharityBeneficiaryTransform(1, Json.toJson(amended), original, LocalDate.parse("2020-03-25"))

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

        val beforeJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-charity-beneficiary-transform-after-declaration.json")

        val amended = BeneficiaryCharityType(
          lineNo = None,
          bpMatchStatus = None,
          "Updated Charity Name",
          None,
          None,
          identification = Some(IdentificationOrgType(
            utr = Some("1234567890"),
            address = None,
            safeId = None
          )),
          None,
          LocalDate.parse("2018-02-28"),
          None
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

        val transformer = AmendCharityBeneficiaryTransform(1, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }

      "amend the new beneficiary that is not known to etmp" in {
        val beforeJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-new-charity-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-new-charity-beneficiary-transform-after-declaration.json")

        val amended = BeneficiaryCharityType(
          lineNo = None,
          bpMatchStatus = None,
          "Amended Charity Name",
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
            |  "organisationName":  "Charity 3",
            |  "entityStart": "2018-02-28"
            |}
            |""".stripMargin)

        val transformer = AmendCharityBeneficiaryTransform(2, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }
    }
  }
}
