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

package utils

import base.BaseSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.JsValue
import utils.JsonOps._

class JsonOpsSpec extends BaseSpec {

  "JsonOps" when {

    "JsValueOps" should {

      "return modified json with no brackets in phone numbers" in {

        val initialJson: JsValue     =
          JsonUtils.getJsonValueFromFile("json-ops-example-data/data-with-brackets-in-phone-numbers.json")
        val reformattedJson: JsValue =
          JsonUtils.getJsonValueFromFile("json-ops-example-data/data-with-reformatted-phone-numbers.json")

        initialJson.applyRules mustBe reformattedJson

      }

      "return modified json with no brackets in agent telephone numbers" in {

        val initialJson: JsValue     =
          JsonUtils.getJsonValueFromFile("json-ops-example-data/data-with-brackets-in-agent-telephone-numbers.json")
        val reformattedJson: JsValue =
          JsonUtils.getJsonValueFromFile("json-ops-example-data/data-with-reformatted-agent-telephone-numbers.json")

        initialJson.applyRules mustBe reformattedJson

      }
    }

    "RemoveRoleInCompanyFields" when {

      ".removeMappedPieces" should {

        "remove beneficiary type field from mapped pieces" in {

          val initialJson: JsValue     =
            JsonUtils.getJsonValueFromFile("json-ops-example-data/beneficiary-mapped-pieces.json")
          val reformattedJson: JsValue = JsonUtils.getJsonValueFromFile(
            "json-ops-example-data/beneficiary-mapped-pieces-with-beneficiary-type-removed.json"
          )

          initialJson.removeMappedPieces() mustBe reformattedJson
        }

        "return original json if no beneficiary type values in mapped pieces" in {

          val initialJson: JsValue     = JsonUtils.getJsonValueFromFile(
            "json-ops-example-data/beneficiary-mapped-pieces-with-beneficiary-type-removed.json"
          )
          val reformattedJson: JsValue = JsonUtils.getJsonValueFromFile(
            "json-ops-example-data/beneficiary-mapped-pieces-with-beneficiary-type-removed.json"
          )

          initialJson.removeMappedPieces() mustBe reformattedJson
        }
      }

      ".removeAnswerRows" should {

        "remove role in company answer from answer rows" in {

          val initialJson: JsValue     =
            JsonUtils.getJsonValueFromFile("json-ops-example-data/beneficiary-answer-rows.json")
          val reformattedJson: JsValue = JsonUtils.getJsonValueFromFile(
            "json-ops-example-data/beneficiary-answer-rows-with-role-in-company-rows-removed.json"
          )

          initialJson.removeAnswerRows() mustBe reformattedJson
        }

        "return original json if no role in company answers in answer rows" in {

          val initialJson: JsValue     = JsonUtils.getJsonValueFromFile(
            "json-ops-example-data/beneficiary-answer-rows-with-role-in-company-rows-removed.json"
          )
          val reformattedJson: JsValue = JsonUtils.getJsonValueFromFile(
            "json-ops-example-data/beneficiary-answer-rows-with-role-in-company-rows-removed.json"
          )

          initialJson.removeAnswerRows() mustBe reformattedJson
        }
      }

      ".removeDraftData" should {

        "remove role in company answers from beneficiaries draft data" in {

          val initialJson: JsValue     = JsonUtils.getJsonValueFromFile("json-ops-example-data/beneficiary-draft-data.json")
          val reformattedJson: JsValue = JsonUtils.getJsonValueFromFile(
            "json-ops-example-data/beneficiary-draft-data-with-role-in-company-answers-removed.json"
          )

          initialJson.removeDraftData() mustBe reformattedJson
        }

        "return original json if no role in company answers in beneficiaries draft data" in {

          val initialJson: JsValue     = JsonUtils.getJsonValueFromFile(
            "json-ops-example-data/beneficiary-draft-data-with-role-in-company-answers-removed.json"
          )
          val reformattedJson: JsValue = JsonUtils.getJsonValueFromFile(
            "json-ops-example-data/beneficiary-draft-data-with-role-in-company-answers-removed.json"
          )

          initialJson.removeDraftData() mustBe reformattedJson
        }
      }
    }
  }

}
