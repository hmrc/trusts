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

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustLeadTrusteeIndType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class PromoteTrusteeIndTransformSpec extends FreeSpec with MustMatchers with OptionValues {
  "the promote trustee ind transformer should" - {
    "successfully promote a trustee to lead and demote the existing lead trustee" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-promote-trustee-transform-before-ind.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-promote-trustee-transform-after-ind.json")
      val newTrusteeInfo = DisplayTrustLeadTrusteeIndType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("John", Some("William"), "O'Connor"),
        dateOfBirth = new DateTime(1956, 2, 12, 12, 30),
        phoneNumber = "Phone",
        email = Some("Email"),
        identification = DisplayTrustIdentificationType(None, Some("ST123456"), None, None),
        entityStart = Some(new DateTime(2000, 1, 1, 12, 30))
      )
      val transformer = PromoteTrusteeIndTransform(index = 0, newLeadTrustee = newTrusteeInfo)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }
  }
}
