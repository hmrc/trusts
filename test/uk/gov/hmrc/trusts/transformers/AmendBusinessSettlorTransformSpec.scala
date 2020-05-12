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
import uk.gov.hmrc.trusts.models.variation.SettlorCompany
import uk.gov.hmrc.trusts.utils.JsonUtils

class AmendBusinessSettlorTransformSpec extends FreeSpec with MustMatchers {

  "AmendBusinessSettlorTransform should" - {

    "before declaration" - {

      "amend a settlors details by replacing" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-business-settlor-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-business-settlor-transform-after.json")

        val amended = SettlorCompany(
          lineNo = None,
          bpMatchStatus = None,
          "New Company",
          None,
          None,
          identification = None,
          LocalDate.parse("2018-02-28"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "lineNo": "1",
            |  "bpMatchStatus": "01",
            |  "name": "Company",
            |  "entityStart": "2018-02-28"
            |}
            |""".stripMargin)

        val transformer = AmendBusinessSettlorTransform(0, Json.toJson(amended), original, LocalDate.parse("2020-03-25"))

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "set an end date for the original beneficiary, adding in the amendment as a new settlor for a settlor known by etmp" in {

        val beforeJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-business-settlor-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-business-settlor-transform-after-declaration.json")

        val amended = SettlorCompany(
          lineNo = None,
          bpMatchStatus = None,
          "New Company",
          None,
          None,
          identification = None,
          LocalDate.parse("2018-02-28"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "lineNo": "1",
            |  "bpMatchStatus": "01",
            |  "name": "Company",
            |  "entityStart": "2018-02-28"
            |}
            |""".stripMargin)

        val transformer = AmendBusinessSettlorTransform(0, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }

      "amend the new settlor that is not known to etmp" in {
        val beforeJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-new-business-settlor-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("transforms/trusts-new-business-settlor-transform-after-declaration.json")

        val amended = SettlorCompany(
          lineNo = None,
          bpMatchStatus = None,
          "Second company updated",
          None,
          None,
          identification = None,
          LocalDate.parse("2020-02-28"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "name": "Second",
            |  "entityStart": "2020-02-28"
            |}
            |""".stripMargin)

        val transformer = AmendBusinessSettlorTransform(1, Json.toJson(amended), original, endDate = LocalDate.parse("2020-03-25"))

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }
    }
  }
}
