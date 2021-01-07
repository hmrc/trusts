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

package transformers.settlors

import models.NameType
import models.variation.AmendDeceasedSettlor
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsValue, Json}
import utils.JsonUtils

class AmendDeceasedSettlorTransformSpec extends FreeSpec with MustMatchers {

  "AmendDeceasedSettlorTransform should" - {

    "before declaration" - {

      "amend a settlors details by replacing it, but retaining their start date, bpMatchStatus and lineNo" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-deceased-settlor-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-deceased-settlor-transform-after.json")

        val amended = AmendDeceasedSettlor(
          name = NameType("updated first", None, "updated last"),
          dateOfBirth = None,
          dateOfDeath = None,
          identification = None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "lineNo":"1",
            |  "bpMatchStatus": "01",
            |  "name":{
            |    "firstName":"John",
            |    "middleName":"William",
            |    "lastName":"O'Connor"
            |  },
            |  "dateOfBirth":"1956-02-12",
            |  "dateOfDeath":"2016-01-01",
            |  "identification":{
            |    "nino":"KC456736"
            |  },
            |  "entityStart":"1998-02-12"
            |}
            |""".stripMargin)

        val transformer = AmendDeceasedSettlorTransform(Json.toJson(amended), original)

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "amend a settlors details by replacing it, but retaining their start date, bpMatchStatus and lineNo" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-deceased-settlor-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-deceased-settlor-transform-after.json")

        val amended = AmendDeceasedSettlor(
          name = NameType("updated first", None, "updated last"),
          dateOfBirth = None,
          dateOfDeath = None,
          identification = None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "lineNo":"1",
            |  "bpMatchStatus": "01",
            |  "name":{
            |    "firstName":"John",
            |    "middleName":"William",
            |    "lastName":"O'Connor"
            |  },
            |  "dateOfBirth":"1956-02-12",
            |  "dateOfDeath":"2016-01-01",
            |  "identification":{
            |    "nino":"KC456736"
            |  },
            |  "entityStart":"1998-02-12"
            |}
            |""".stripMargin)

        val transformer = AmendDeceasedSettlorTransform(Json.toJson(amended), original)

        val applied = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(applied).get
        result mustBe afterJson
      }
    }
  }
}
