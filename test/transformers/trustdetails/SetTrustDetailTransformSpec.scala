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

package transformers.trustdetails

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.{JsBoolean, JsString, Json}
import utils.JsonUtils

import java.time.LocalDate

class SetTrustDetailTransformSpec extends AnyFreeSpec {

  "the set trust detail transformer" - {

    "when express" - {

      val detailType = "expressTrust"

      "before declaration" - {
        "amend trust details by setting the field" - {

          "when the field already exists" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before-populated-express.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-express.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }

          "when it is a new value" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-express.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }
        }
      }
    }

    "when property" - {

      val detailType = "trustUKProperty"

      "before declaration" - {
        "amend trust details by setting the field" - {

          "when the field already exists" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before-populated-property.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-property.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }

          "when it is a new value" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-property.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }
        }
      }
    }

    "when recorded" - {

      val detailType = "trustRecorded"

      "before declaration" - {
        "amend trust details by setting the field" - {

          "when the field already exists" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before-populated-recorded.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-recorded.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }

          "when it is a new value" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-recorded.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }
        }
      }
    }

    "when resident" - {

      val detailType = "trustUKResident"

      "before declaration" - {
        "amend trust details by setting the field" - {

          "when the field already exists" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before-populated-resident.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-resident.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }

          "when it is a new value" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-resident.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }
        }
      }
    }

    "when taxable" - {

      val detailType = "trustTaxable"

      "before declaration" - {
        "amend trust details by setting the field" - {

          "when the field already exists" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before-populated-taxable.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-taxable.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }

          "when it is a new value" in {

            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-taxable.json")

            val transformer = SetTrustDetailTransform(JsBoolean(false), detailType)

            val result = transformer.applyTransform(beforeJson).get
            result mustBe afterJson
          }
        }
      }
    }

    "when deedOfVariation" - {

      val deedOfVariation = "Previously there was only an absolute interest under the will"

      "before declaration" - {
        "set the field and remove interVivos and efrbsStartDate" in {

          val beforeJson = Json.parse(
            """
              |{
              |  "details": {
              |    "trust": {
              |      "details": {
              |        "interVivos": false,
              |        "efrbsStartDate": "2001-01-01"
              |      }
              |    }
              |  }
              |}
              |""".stripMargin
          )

          val afterJson = Json.parse(
            s"""
              |{
              |  "details": {
              |    "trust": {
              |      "details": {
              |        "deedOfVariation": "$deedOfVariation"
              |      }
              |    }
              |  }
              |}
              |""".stripMargin
          )

          val transformer = SetTrustDetailTransform(JsString(deedOfVariation), "deedOfVariation")

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }

    "when interVivos" - {

      val interVivos = false

      "before declaration" - {
        "set the field and remove deedOfVariation and efrbsStartDate" in {

          val beforeJson = Json.parse(
            """
              |{
              |  "details": {
              |    "trust": {
              |      "details": {
              |        "deedOfVariation": "Previously there was only an absolute interest under the will",
              |        "efrbsStartDate": "2001-01-01"
              |      }
              |    }
              |  }
              |}
              |""".stripMargin
          )

          val afterJson = Json.parse(
            s"""
              |{
              |  "details": {
              |    "trust": {
              |      "details": {
              |        "interVivos": $interVivos
              |      }
              |    }
              |  }
              |}
              |""".stripMargin
          )

          val transformer = SetTrustDetailTransform(JsBoolean(interVivos), "interVivos")

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }

    "when efrbsStartDate" - {

      val efrbsStartDate = "2001-01-01"

      "before declaration" - {
        "set the field and remove deedOfVariation and efrbsStartDate" in {

          val beforeJson = Json.parse(
            """
              |{
              |  "details": {
              |    "trust": {
              |      "details": {
              |        "deedOfVariation": "Previously there was only an absolute interest under the will",
              |        "interVivos": false
              |      }
              |    }
              |  }
              |}
              |""".stripMargin
          )

          val afterJson = Json.parse(
            s"""
               |{
               |  "details": {
               |    "trust": {
               |      "details": {
               |        "efrbsStartDate": "$efrbsStartDate"
               |      }
               |    }
               |  }
               |}
               |""".stripMargin
          )

          val transformer = SetTrustDetailTransform(Json.toJson(LocalDate.parse(efrbsStartDate)), "efrbsStartDate")

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }
  }
}
