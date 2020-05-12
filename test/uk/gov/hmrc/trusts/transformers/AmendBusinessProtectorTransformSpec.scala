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
import uk.gov.hmrc.trusts.models.IdentificationOrgType
import uk.gov.hmrc.trusts.models.variation.{BeneficiaryCompanyType, ProtectorCompany}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AmendBusinessProtectorTransformSpec extends FreeSpec with MustMatchers {

  "AmendBusinessProtectorTransform should" - {

    "before declaration" - {

      "amend a protector's details by replacing the protector" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("trusts-business-protector-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-business-protector-transform-after.json")

        val amended = ProtectorCompany(
          lineNo = None,
          bpMatchStatus = None,
          "Company Name",
          identification = Some(IdentificationOrgType(
            utr = Some("1234567890"),
            address = None
          )),
          LocalDate.parse("2018-02-28"),
          None
        )

        val original: JsValue = Json.parse(
          """
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

        val transformer = AmendBusinessProtectorTransform(1, Json.toJson(amended), original, LocalDate.parse("2020-03-25"))

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
          identification = Some(IdentificationOrgType(
            utr = Some("1234567890"),
            address = None
          )),
          LocalDate.parse("2018-02-28"),
          None
        )

        val original: JsValue = Json.parse(
          """
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

        val transformer = AmendBusinessProtectorTransform(1, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
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
          LocalDate.parse("2018-02-28"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "name": "Company 3",
            |  "entityStart": "2018-02-28"
            |}
            |""".stripMargin)

        val transformer = AmendBusinessProtectorTransform(2, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }
    }
  }
}
