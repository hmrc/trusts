/*
 * Copyright 2023 HM Revenue & Customs
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

package transformers.protectors

import models.NameType
import models.variation.{IdentificationType, ProtectorCompany, ProtectorIndividual}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class AddProtectorTransformSpec extends AnyFreeSpec {

  "the add protector transformer" - {

    "should add a new protector" - {

      "when individual" - {

        val newProtector = ProtectorIndividual(Some("1"),
          None,
          NameType("abcdefghijkl", Some("abcdefghijklmn"), "abcde"),
          Some(LocalDate.parse("2000-01-01")),
          Some(IdentificationType(Some("ST019091"), None, None, None)),
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None,
          LocalDate.parse("2002-01-01"),
          None
        )

        val newSecondProtector = ProtectorIndividual(None,
          None,
          NameType("second", None, "protector"),
          Some(LocalDate.parse("2000-01-01")),
          Some(IdentificationType(Some("AB123456"), None, None, None)),
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None,
          LocalDate.parse("2010-01-01"),
          None
        )

        val `type` = "protector"

        "must add a new individual protector when there are no existing individual protectors" in {
          val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-no-protectors.json")

          val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-individual-protector.json")

          val transformer = new AddProtectorTransform(Json.toJson(newProtector), `type`)

          val result = transformer.applyTransform(trustJson).get

          result mustBe afterJson
        }

        "must add a new individual protector" in {
          val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-individual-protector.json")

          val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-individual-protector.json")

          val transformer = new AddProtectorTransform(Json.toJson(newSecondProtector), `type`)

          val result = transformer.applyTransform(trustJson).get

          result mustBe afterJson
        }
      }

      "when business" - {

        val newProtector = ProtectorCompany(
          name = "TestCompany",
          identification = None,
          lineNo = None,
          bpMatchStatus = None,
          countryOfResidence = None,
          entityStart = LocalDate.parse("2010-05-03"),
          entityEnd = None
        )

        val newSecondProtector = ProtectorCompany(
          name = "TheNewOne",
          identification = None,
          lineNo = None,
          bpMatchStatus = None,
          countryOfResidence = None,
          entityStart = LocalDate.parse("2019-01-01"),
          entityEnd = None
        )

        val `type` = "protectorCompany"

        "must add a new business protector when there are no existing business protectors" in {
          val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-no-protectors.json")

          val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-company-protector.json")

          val transformer = new AddProtectorTransform(Json.toJson(newProtector), `type`)

          val result = transformer.applyTransform(trustJson).get

          result mustBe afterJson
        }

        "must add a new business protector" in {
          val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-company-protector.json")

          val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-company-protector.json")

          val transformer = new AddProtectorTransform(Json.toJson(newSecondProtector), `type`)

          val result = transformer.applyTransform(trustJson).get

          result mustBe afterJson
        }
      }
    }
  }
}