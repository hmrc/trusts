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
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustProtector, DisplayTrustProtectorCompany}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddCompanyProtectorTransformSpec extends FreeSpec with MustMatchers {

  val newCompanyProtector = DisplayTrustProtectorCompany(
    name = "TestCompany",
    identification = None,
    lineNo = None,
    bpMatchStatus = None,
    countryOfResidence = None,
    entityStart = LocalDate.parse("2010-05-03")
  )

  val newSecondCompanyProtector = DisplayTrustProtectorCompany(
    name = "TheNewOne",
    identification = None,
    lineNo = None,
    bpMatchStatus = None,
    countryOfResidence = None,
    entityStart = LocalDate.parse("2019-01-01")
  )

  "the add company protector transformer should" - {

    "add a new company protector when there are no company protectors existing" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-no-protectors.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-company-protector.json")

      val transformer = new AddCompanyProtectorTransform(newCompanyProtector)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new company protector" in {

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-company-protector.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-company-protector.json")

      val transformer = new AddCompanyProtectorTransform(newSecondCompanyProtector)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}