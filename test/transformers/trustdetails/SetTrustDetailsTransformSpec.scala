/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.libs.json.Json
import utils.JsonUtils

class SetTrustDetailsTransformSpec extends AnyFreeSpec {

  "the set trust detail transformer" - {

    "when not migrating" - {
      "before declaration" - {
        "merge the old details with the new details" in {

          val beforeJson =
            JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before-non-migration.json")
          val afterJson  = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-non-migration.json")

          val newTrustDetails = Json.parse(
            """
              |{
              |  "trustUKProperty": true,
              |  "trustRecorded": true,
              |  "trustUKResident": true
              |}
              |""".stripMargin
          )

          val transformer = SetTrustDetailsTransform(newTrustDetails, migratingFromNonTaxableToTaxable = false)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }

    "when migrating" - {
      "before declaration" - {
        "merge the old details with the new details" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before-migration.json")
          val afterJson  = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-migration.json")

          val newTrustDetails = Json.parse(
            """
              |{
              |  "lawCountry": "FR",
              |  "schedule3aExempt": true,
              |  "administrationCountry": "GB",
              |  "residentialStatus": {
              |    "nonUK": {
              |      "sch5atcgga92": true
              |    }
              |  },
              |  "trustUKProperty": true,
              |  "trustRecorded": true,
              |  "trustUKRelation": false,
              |  "trustUKResident": false,
              |  "typeOfTrust": "Inter vivos Settlement",
              |  "interVivos": true
              |}
              |""".stripMargin
          )

          val transformer = SetTrustDetailsTransform(newTrustDetails, migratingFromNonTaxableToTaxable = true)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }
  }

}
