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

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.trusts.models.AddressType
import uk.gov.hmrc.trusts.models.variation.OtherType
import uk.gov.hmrc.trusts.utils.JsonUtils

class AmendOtherBeneficiaryTransformSpec extends FreeSpec with MustMatchers {

  "AmendOtherBeneficiaryTransform should" - {

    "before declaration" - {

      "amend a beneficiaries details by replacing the beneficiary" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("trusts-other-beneficiary-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-other-beneficiary-transform-after.json")

        val amended = OtherType(
          lineNo = None,
          bpMatchStatus = None,
          "Amended Description",
          Some(AddressType("Amended House", "Amended Street", None, None, Some("NE1 1EN"), "GB")),
          Some(false),
          None,
          LocalDate.parse("2019-02-12"),
          None
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

        val transformer = AmendOtherBeneficiaryTransform(1, Json.toJson(amended), original, LocalDate.parse("2020-03-25"))

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

        val beforeJson =
          JsonUtils.getJsonValueFromFile("trusts-other-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("trusts-other-beneficiary-transform-after-declaration.json")

        val amended = OtherType(
          lineNo = None,
          bpMatchStatus = None,
          "Amended Description",
          Some(AddressType("Amended House", "Amended Street", None, None, Some("NE1 1EN"), "GB")),
          Some(false),
          None,
          LocalDate.parse("2019-02-12"),
          None
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

        val transformer = AmendOtherBeneficiaryTransform(1, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }

      "amend the new beneficiary that is not known to etmp" in {
        val beforeJson =
          JsonUtils.getJsonValueFromFile("trusts-new-other-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("trusts-new-other-beneficiary-transform-after-declaration.json")

        val amended = OtherType(
          lineNo = None,
          bpMatchStatus = None,
          "Amended Description",
          Some(AddressType("Amended House", "Amended Street", None, None, Some("NE1 1EN"), "GB")),
          Some(false),
          None,
          LocalDate.parse("2020-02-12"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "description":"New 3",
            |  "entityStart":"2020-02-12"
            |}
            |""".stripMargin)

        val transformer = AmendOtherBeneficiaryTransform(2, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-12"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }
    }
  }
}
