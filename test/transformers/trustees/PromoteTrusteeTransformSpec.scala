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

package transformers.trustees

import models.NameType
import models.variation.{AmendedLeadTrusteeIndType, AmendedLeadTrusteeOrgType, IdentificationOrgType, IdentificationType}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class PromoteTrusteeTransformSpec extends FreeSpec with MustMatchers {

  "the promote trustee transformer when" - {

    val endDate: LocalDate = LocalDate.parse("2020-02-28")

    "individual trustee should" - {

      def transformToTest: PromoteTrusteeTransform = {

        val trustee0Json = Json.parse(
          """
            |{
            |  "trusteeInd": {
            |    "lineNo": "1",
            |    "name": {
            |      "firstName": "John",
            |      "middleName": "William",
            |      "lastName": "O'Connor"
            |    },
            |    "dateOfBirth": "1956-02-12",
            |    "identification": {
            |      "nino": "ST123456"
            |    },
            |    "legallyIncapable": false,
            |    "nationality": "FR",
            |    "countryOfResidence": "FR",
            |    "entityStart": "2000-01-01"
            |  }
            |}
            |""".stripMargin)

        val newTrusteeInfo = AmendedLeadTrusteeIndType(
          name = NameType("John", Some("William"), "O'Connor"),
          dateOfBirth = LocalDate.of(1956, 2, 12),
          phoneNumber = "Phone",
          email = Some("Email"),
          identification = IdentificationType(Some("ST123456"), None, None, None),
          countryOfResidence = Some("FR"),
          legallyIncapable = Some(false),
          nationality = Some("FR")
        )

        PromoteTrusteeTransform(0, Json.toJson(newTrusteeInfo), trustee0Json, endDate, "trusteeInd")
      }

      "successfully promote a trustee to lead and demote the existing lead trustee" in {
        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-before-ind.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind.json")

        val result = transformToTest.applyTransform(beforeJson).get
        result mustBe afterJson
      }

      "re-add the removed trustee with an end date at declaration time if it existed before" in {
        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind-declare.json")

        val result = transformToTest.applyDeclarationTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "business trustee should" - {

      def transformToTest: PromoteTrusteeTransform = {

        val trustee1Json = Json.parse(
          """
            |{
            |  "trusteeOrg": {
            |    "name": "Trustee Org 1",
            |    "phoneNumber": "0121546546",
            |    "identification": {
            |      "utr": "5465416546"
            |    },
            |    "countryOfResidence": "DE",
            |    "entityStart":"1999-01-01"
            |  }
            |}
            |""".stripMargin)

        val newTrusteeInfo = AmendedLeadTrusteeOrgType(
          name = "Trustee Org 1",
          phoneNumber = "0121546546",
          email = None,
          identification = IdentificationOrgType(Some("5465416546"), None, None),
          countryOfResidence = Some("DE")
        )

        PromoteTrusteeTransform(1, Json.toJson(newTrusteeInfo), trustee1Json, endDate, "trusteeOrg")
      }

      "successfully promote a trustee to lead and demote the existing lead trustee" in {
        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-before-ind.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-org.json")
        val result = transformToTest.applyTransform(beforeJson).get
        result mustBe afterJson
      }

      "not re-add the removed trustee with if it didn't exist before" in {
        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-org.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-org.json")

        val result = transformToTest.applyDeclarationTransform(beforeJson).get
        result mustBe afterJson
      }
    }
  }
}
