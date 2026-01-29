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

package transformers.settlors

import models.NameType
import models.variation.{IdentificationOrgType, IdentificationType, SettlorCompany, SettlorIndividual}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class AddSettlorTransformSpec extends AnyFreeSpec {

  "the add settlor transformer" - {

    "should add a new settlor" - {

      "when individual" - {

        val newSettlor = SettlorIndividual(
          lineNo = Some("1"),
          bpMatchStatus = None,
          name = NameType("abcdefghijkl", Some("abcdefghijklmn"), "abcde"),
          dateOfBirth = Some(LocalDate.parse("2000-01-01")),
          identification = Some(IdentificationType(Some("ST019091"), None, None, None)),
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None,
          entityStart = LocalDate.parse("2002-01-01"),
          entityEnd = None
        )

        val `type` = "settlor"

        "must add a new individual settlor when there are no existing individual settlors" in {
          val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-settlor.json")

          val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-individual-settlor.json")

          val transformer = new AddSettlorTransform(Json.toJson(newSettlor), `type`)

          val result = transformer.applyTransform(trustJson).get

          result mustBe afterJson
        }

        "must add a new individual settlor" in {
          val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

          val afterJson =
            JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-individual-settlor.json")

          val transformer = new AddSettlorTransform(Json.toJson(newSettlor), `type`)

          val result = transformer.applyTransform(trustJson).get

          result mustBe afterJson
        }
      }

      "when business" - {

        val newSettlor = SettlorCompany(
          lineNo = Some("1"),
          bpMatchStatus = None,
          name = "Test",
          companyType = None,
          companyTime = Some(false),
          identification = Some(IdentificationOrgType(Some("ST019091"), None, None)),
          countryOfResidence = None,
          entityStart = LocalDate.parse("2002-01-01"),
          entityEnd = None
        )

        val `type` = "settlorCompany"

        "must add a new business settlor when there are no existing business settlors" in {
          val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-no-business-settlor.json")

          val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-add-new-business-settlor.json")

          val transformer = new AddSettlorTransform(Json.toJson(newSettlor), `type`)

          val result = transformer.applyTransform(trustJson).get

          result mustBe afterJson
        }

        "must add a new business settlor" in {
          val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-add-new-business-settlor.json")

          val afterJson =
            JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-add-second-business-settlor.json")

          val transformer = new AddSettlorTransform(Json.toJson(newSettlor), `type`)

          val result = transformer.applyTransform(trustJson).get

          result mustBe afterJson
        }
      }
    }
  }

}
