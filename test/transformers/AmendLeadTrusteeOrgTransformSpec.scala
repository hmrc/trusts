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

import org.scalatest.{FreeSpec, MustMatchers}
import models.variation.{AmendedLeadTrusteeOrgType, IdentificationOrgType}
import transformers.trustees.AmendLeadTrusteeOrgTransform
import utils.JsonUtils

class AmendLeadTrusteeOrgTransformSpec extends FreeSpec with MustMatchers {

  "the modify lead transformer should" - {

    "successfully set a new org lead trustee's details" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-after-org.json")
      val newTrusteeInfo = AmendedLeadTrusteeOrgType(
        name = "newName",
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = IdentificationOrgType( Some("newUtr"), None, None),
        countryOfResidence = None
      )
      val transformer = AmendLeadTrusteeOrgTransform(newTrusteeInfo)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }

  }
}
