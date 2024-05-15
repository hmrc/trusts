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

package transformers.trustees

import models.NameType
import models.variation._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json.{JsValue, Json}
import utils.JsonUtils

import java.time.LocalDate

class PromoteTrusteeTransformSpec extends AnyFreeSpec {

  "the promote trustee transformer when" - {

    val endDate: LocalDate = LocalDate.parse("2020-02-28")

    "promoting individual trustee when" - {

      val trusteeBeforePromotionTaxable = Json.parse(
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
          |""".stripMargin
      )

      val trusteeBeforePromotionNonTaxable = Json.parse(
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
          |    "legallyIncapable": false,
          |    "nationality": "FR",
          |    "countryOfResidence": "FR",
          |    "entityStart": "2000-01-01"
          |  }
          |}
          |""".stripMargin
      )

      "not fully matched should" - {

        def transformToTest(trusteeBeforePromotion: JsValue, isTaxable: Boolean): PromoteTrusteeTransform = {

          val trusteeAfterPromotion = AmendedLeadTrusteeIndType(
            bpMatchStatus = None,
            name = NameType("John", Some("William"), "O'Connor"),
            dateOfBirth = LocalDate.of(1956, 2, 12),
            phoneNumber = "Phone",
            email = Some("Email"),
            identification = IdentificationType(Some("ST123456"), None, None, None),
            countryOfResidence = Some("FR"),
            legallyIncapable = Some(false),
            nationality = Some("FR")
          )

          PromoteTrusteeTransform(Some(0), Json.toJson(trusteeAfterPromotion), trusteeBeforePromotion, endDate, "trusteeInd", isTaxable)
        }

        "successfully promote a trustee to lead and demote the existing lead trustee" - {

          "taxable" in {
            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-before-ind.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind.json")

            val result = transformToTest(trusteeBeforePromotionTaxable, isTaxable = true).applyTransform(beforeJson).get
            result mustBe afterJson
          }

          "non-taxable" in {
            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-before-ind-non-taxable.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind-non-taxable.json")

            val result = transformToTest(trusteeBeforePromotionNonTaxable, isTaxable = false).applyTransform(beforeJson).get
            result mustBe afterJson
          }

        }

        "re-add the removed trustee with an end date at declaration time if it existed before (i.e. has a line no.)" in {
          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind-declare.json")

          val result = transformToTest(trusteeBeforePromotionTaxable, isTaxable = true).applyDeclarationTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "fully matched should" - {

        def transformToTest(trusteeBeforePromotion: JsValue, isTaxable: Boolean): PromoteTrusteeTransform = {

          val trusteeAfterPromotion = AmendedLeadTrusteeIndType(
            bpMatchStatus = Some("01"),
            name = NameType("John", Some("William"), "O'Connor"),
            dateOfBirth = LocalDate.of(1956, 2, 12),
            phoneNumber = "Phone",
            email = Some("Email"),
            identification = IdentificationType(Some("ST123456"), None, None, None),
            countryOfResidence = Some("FR"),
            legallyIncapable = Some(false),
            nationality = Some("FR")
          )

          PromoteTrusteeTransform(Some(0), Json.toJson(trusteeAfterPromotion), trusteeBeforePromotion, endDate, "trusteeInd", isTaxable)
        }

        "successfully promote a trustee to lead (persisting the match status) and demote the existing lead trustee" - {

          "taxable" in {
            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-before-ind.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind-fully-matched.json")

            val result = transformToTest(trusteeBeforePromotionTaxable, isTaxable = true).applyTransform(beforeJson).get
            result mustBe afterJson
          }

          "non-taxable" in {
            val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-before-ind-non-taxable.json")
            val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind-non-taxable-fully-matched.json")

            val result = transformToTest(trusteeBeforePromotionNonTaxable, isTaxable = false).applyTransform(beforeJson).get
            result mustBe afterJson
          }

        }

        "re-add the removed trustee with an end date at declaration time if it existed before (i.e. has a line no.)" in {
          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind-fully-matched.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-ind-declare.json")

          val result = transformToTest(trusteeBeforePromotionTaxable, isTaxable = true).applyDeclarationTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }

    "promoting business trustee should" - {

      val trusteeBeforePromotionTaxable = Json.parse(
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
          |""".stripMargin
      )

      def transformToTest(trusteeBeforePromotion: JsValue, isTaxable: Boolean): PromoteTrusteeTransform = {

        val trusteeAfterPromotion = AmendedLeadTrusteeOrgType(
          name = "Trustee Org 1",
          phoneNumber = "0121546546",
          email = None,
          identification = IdentificationOrgType(Some("5465416546"), None, None),
          countryOfResidence = Some("DE")
        )

        PromoteTrusteeTransform(Some(1), Json.toJson(trusteeAfterPromotion), trusteeBeforePromotion, endDate, "trusteeOrg", isTaxable)
      }

      "successfully promote a trustee to lead and demote the existing lead trustee" - {

        "taxable" in {
          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-before-org.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-org.json")
          val result = transformToTest(trusteeBeforePromotionTaxable, isTaxable = true).applyTransform(beforeJson).get
          result mustBe afterJson
        }

        "non-taxable" in {
          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-before-org-non-taxable.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-org-non-taxable.json")
          val result = transformToTest(trusteeBeforePromotionTaxable, isTaxable = false).applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "not re-add the removed trustee if it didn't exist before (i.e. doesn't have a line no.)" in {
        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-org.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-promote-trustee-transform-after-org.json")

        val result = transformToTest(trusteeBeforePromotionTaxable, isTaxable = true).applyDeclarationTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "demoting individual lead trustee should" - {

      val newLeadTrustee = LeadTrusteeIndType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("Johnny", Some("Jonathan"), "Doe"),
        dateOfBirth = LocalDate.parse("1996-02-03"),
        phoneNumber = "0987654321",
        email = Some("johnny.doe@example.com"),
        identification = IdentificationType(Some("AB000000A"), None, None, None),
        countryOfResidence = Some("FR"),
        legallyIncapable = Some(false),
        nationality = Some("FR"),
        entityStart = LocalDate.parse("2016-02-23"),
        entityEnd = None
      )

      "successfully add a new lead trustee and demote the existing lead trustee" - {

        "taxable" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-before-ind.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-ind.json")
          val transform = PromoteTrusteeTransform(None, Json.toJson(newLeadTrustee), Json.obj(), endDate, "leadTrusteeInd", isTaxable = true)
          val result = transform.applyTransform(beforeJson).get
          result mustBe afterJson
        }

        "non-taxable" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-before-ind.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-ind-non-taxable.json")
          val transform = PromoteTrusteeTransform(None, Json.toJson(newLeadTrustee), Json.obj(), endDate, "leadTrusteeInd", isTaxable = false)
          val result = transform.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "do nothing extra at declaration time" - {

        "taxable" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-ind.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-ind.json")
          val transform = PromoteTrusteeTransform(None, Json.toJson(newLeadTrustee), Json.obj(), endDate, "leadTrusteeInd", isTaxable = true)
          val result = transform.applyDeclarationTransform(beforeJson).get
          result mustBe afterJson
        }

        "non-taxable" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-ind-non-taxable.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-ind-non-taxable.json")
          val transform = PromoteTrusteeTransform(None, Json.toJson(newLeadTrustee), Json.obj(), endDate, "leadTrusteeInd", isTaxable = false)
          val result = transform.applyDeclarationTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }

    "demoting business lead trustee should" - {

      val newLeadTrustee = LeadTrusteeOrgType(
        lineNo = None,
        bpMatchStatus = None,
        name = "Amazon",
        phoneNumber = "0987654321",
        email = Some("amazon@example.com"),
        identification = IdentificationOrgType(Some("5432109876"), None, None),
        countryOfResidence = Some("FR"),
        entityStart = LocalDate.parse("2016-02-23"),
        entityEnd = None
      )

      "successfully add a new lead trustee and demote the existing lead trustee" - {

        "taxable" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-before-org.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-org.json")
          val transform = PromoteTrusteeTransform(None, Json.toJson(newLeadTrustee), Json.obj(), endDate, "leadTrusteeOrg", isTaxable = true)
          val result = transform.applyTransform(beforeJson).get
          result mustBe afterJson
        }

        "non-taxable" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-before-org.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-org-non-taxable.json")
          val transform = PromoteTrusteeTransform(None, Json.toJson(newLeadTrustee), Json.obj(), endDate, "leadTrusteeOrg", isTaxable = false)
          val result = transform.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "do nothing extra at declaration time" - {

        "taxable" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-org.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-org.json")
          val transform = PromoteTrusteeTransform(None, Json.toJson(newLeadTrustee), Json.obj(), endDate, "leadTrusteeOrg", isTaxable = true)
          val result = transform.applyDeclarationTransform(beforeJson).get
          result mustBe afterJson
        }

        "non-taxable" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-org-non-taxable.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-add-new-lead-trustee-transform-after-org-non-taxable.json")
          val transform = PromoteTrusteeTransform(None, Json.toJson(newLeadTrustee), Json.obj(), endDate, "leadTrusteeOrg", isTaxable = false)
          val result = transform.applyDeclarationTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }
  }
}
