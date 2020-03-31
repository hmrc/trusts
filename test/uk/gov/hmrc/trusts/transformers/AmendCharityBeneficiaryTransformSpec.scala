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

import java.time.LocalDate

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.trusts.models.IdentificationOrgType
import uk.gov.hmrc.trusts.models.variation.CharityType
import uk.gov.hmrc.trusts.utils.JsonUtils

class AmendCharityBeneficiaryTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  "AmendCharityBeneficiaryTransform should" - {

    "before declaration" - {

      "amend a beneficiaries details by replacing the beneficiary" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("trusts-charity-beneficiary-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-charity-beneficiary-transform-after.json")

        val amended = CharityType(
          lineNo = Some("2"),
          bpMatchStatus = Some("01"),
          "Charity Name",
          None,
          None,
          identification = Some(IdentificationOrgType(
            utr = Some("1234567890"),
            address = None
          )),
          DateTime.parse("2010-01-01"),
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

        val transformer = AmendCharityBeneficiaryTransform(1, amended, original, LocalDate.parse("2020-03-25"))

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

        val beforeJson =
          JsonUtils.getJsonValueFromFile("trusts-charity-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("trusts-charity-beneficiary-transform-after-declaration.json")

        val amended = CharityType(
          lineNo = Some("2"),
          bpMatchStatus = Some("01"),
          "Updated Charity Name",
          None,
          None,
          identification = Some(IdentificationOrgType(
            utr = Some("1234567890"),
            address = None
          )),
          DateTime.parse("2010-01-01"),
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

        val transformer = AmendCharityBeneficiaryTransform(1, amended, original, endDate = LocalDate.parse("2020-03-25"))

        val result = transformer.applyDeclarationTransform(beforeJson).get
        result mustBe afterJson
      }

      "amend the new beneficiary that is not known to etmp" in {
        val beforeJson =
          JsonUtils.getJsonValueFromFile("trusts-new-charity-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("trusts-new-charity-beneficiary-transform-after-declaration.json")

        val amended = CharityType(
          lineNo = None,
          bpMatchStatus = None,
          "Amended Charity Name",
          None,
          None,
          None,
          DateTime.parse("2018-02-28"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "organisationName":  "Charity 3",
            |  "entityStart": "2018-02-28"
            |}
            |""".stripMargin)

        val transformer = AmendCharityBeneficiaryTransform(2, amended, original, endDate = LocalDate.parse("2020-03-25"))

        val result = transformer.applyDeclarationTransform(beforeJson).get
        result mustBe afterJson
      }

    }

  }

}