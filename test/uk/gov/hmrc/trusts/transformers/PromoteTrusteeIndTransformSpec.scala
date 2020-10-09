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

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.variation.{IdentificationType, LeadTrusteeIndType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class PromoteTrusteeIndTransformSpec extends FreeSpec with MustMatchers {

  private val trustee0Json = Json.parse(
    """
      |{
      |   "trusteeInd":{
      |     "lineNo": "1",
      |     "name":{
      |       "firstName":"John",
      |       "middleName":"William",
      |       "lastName":"O'Connor"
      |     },
      |     "dateOfBirth":"1956-02-12",
      |     "identification":{
      |       "nino":"ST123456"
      |     },
      |     "entityStart":"2000-01-01"
      |   }
      |}
      |""".stripMargin)

  "the promote trustee ind transformer should" - {

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

  private def transformToTest = {
    val newTrusteeInfo = LeadTrusteeIndType(
      lineNo = None,
      bpMatchStatus = None,
      name = NameType("John", Some("William"), "O'Connor"),
      dateOfBirth = LocalDate.of(1956, 2, 12),
      phoneNumber = "Phone",
      email = Some("Email"),
      identification = IdentificationType(Some("ST123456"), None, None, None),
      countryOfResidence = None,
      legallyIncapable = None,
      nationality = None,
      entityStart = LocalDate.of(2010, 4, 3),
      entityEnd = None
    )
    PromoteTrusteeIndTransform(
      index = 0,
      newLeadTrustee = newTrusteeInfo,
      LocalDate.of(2020, 2, 28),
      trustee0Json,
      LocalDate.of(2020, 4, 1))
  }
}
