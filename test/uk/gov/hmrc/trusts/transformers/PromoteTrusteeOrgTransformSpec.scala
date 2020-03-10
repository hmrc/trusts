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

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationOrgType, DisplayTrustLeadTrusteeOrgType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class PromoteTrusteeOrgTransformSpec extends FreeSpec with MustMatchers with OptionValues {
  private val trustee1Json = Json.parse(
    """
      |
      |{
      |  "trusteeOrg": {
      |    "name": "Trustee Org 1",
      |    "phoneNumber": "0121546546",
      |    "identification": {
      |      "utr": "5465416546"
      |    },
      |    "entityStart":"1999-01-01"
      |  }
      |}
      |""".stripMargin)

  "the promote trustee org transformer should" - {

    "successfully promote a trustee to lead and demote the existing lead trustee" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-promote-trustee-transform-before-ind.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-promote-trustee-transform-after-org.json")
      val result = transformToTest.applyTransform(beforeJson).get
      result mustBe afterJson
    }
  }

  "not re-add the removed trustee with if it didn't exist before" in {
    val beforeJson = JsonUtils.getJsonValueFromFile("trusts-promote-trustee-transform-after-org.json")
    val afterJson = JsonUtils.getJsonValueFromFile("trusts-promote-trustee-transform-after-org.json")

    val result = transformToTest.applyDeclarationTransform(beforeJson).get
    result mustBe afterJson
  }

  private def transformToTest = {
    val newTrusteeInfo = DisplayTrustLeadTrusteeOrgType(
      lineNo = None,
      bpMatchStatus = None,
      name = "Trustee Org 1",
      phoneNumber = "0121546546",
      email = None,
      identification = DisplayTrustIdentificationOrgType(None, Some("5465416546"), None),
      entityStart = None
    )
    val transformer = PromoteTrusteeOrgTransform(index = 1, newLeadTrustee = newTrusteeInfo, LocalDate.of(2020, 2, 28), trustee1Json)
    transformer
  }
}
