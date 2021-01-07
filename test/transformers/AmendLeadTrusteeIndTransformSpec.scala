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

package transformers

import java.time.LocalDate
import org.scalatest.{FreeSpec, MustMatchers}
import models.NameType
import models.variation.{AmendedLeadTrusteeIndType, IdentificationType}
import transformers.trustees.AmendLeadTrusteeIndTransform
import utils.JsonUtils

class AmendLeadTrusteeIndTransformSpec extends FreeSpec with MustMatchers {
  "the modify lead transformer should" - {
    "successfully set a new ind lead trustee's details" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-after-ind.json")
      val newTrusteeInfo = AmendedLeadTrusteeIndType(
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = LocalDate.of(1965, 2, 10),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = IdentificationType(Some("newNino"), None, None, None),
        countryOfResidence = None,
        legallyIncapable = None,
        nationality = None
      )
      val transformer = AmendLeadTrusteeIndTransform(newTrusteeInfo)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }
  }
}
