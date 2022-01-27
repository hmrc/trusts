/*
 * Copyright 2022 HM Revenue & Customs
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

package transformers.trustees

import models.NameType
import models.variation.{IdentificationType, TrusteeIndividualType, TrusteeOrgType}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class AddTrusteeTransformSpec extends AnyFreeSpec {

  "the add trustee transformer when" - {

    "individual trustee should" - {

      val t = TrusteeIndividualType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("New", None, "Trustee"),
        dateOfBirth = Some(LocalDate.parse("2000-01-01")),
        phoneNumber = Some("phoneNumber"),
        identification = Some(IdentificationType(Some("nino"), None, None, None)),
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None,
        entityStart = LocalDate.parse("1990-10-10"),
        entityEnd = None
      )

      "add a new individual trustee when there are no trustees existing" in {

        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-no-trustees.json")

        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-trustees-after-add.json")

        val transformer = AddTrusteeTransform(Json.toJson(t), "trusteeInd")

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "add a new individual trustee" in {

        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-add-ind-trustee.json")

        val transformer = AddTrusteeTransform(Json.toJson(t), "trusteeInd")

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

    "business trustee should" - {

      val t = TrusteeOrgType(
        lineNo = None,
        bpMatchStatus = None,
        name = "Company Name",
        phoneNumber = None,
        email = None,
        identification = None,
        countryOfResidence = None,
        entityStart = LocalDate.parse("2020-01-30"),
        entityEnd = None
      )

      "add a new organisation trustee when there are no trustees existing" in {

        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-no-trustees.json")

        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-trustees-after-org-add.json")

        val transformer = AddTrusteeTransform(Json.toJson(t), "trusteeOrg")

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "add a new org trustee" in {

        val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

        val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-add-org-trustee.json")

        val transformer = AddTrusteeTransform(Json.toJson(t), "trusteeOrg")

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }
  }
}