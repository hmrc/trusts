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

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AmendUnidentifiedBeneficiaryTransformSpec extends FreeSpec with MustMatchers with OptionValues {
  "AmendUnidentifiedBeneficiaryTransform should" - {
    "successfully update a beneficiary's details" in {

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-unidentified-beneficiary-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-unidentified-beneficiary-transform-after.json")
      val newDescription = "This description has been updated"
      val transformer = AmendUnidentifiedBeneficiaryTransform(1, newDescription)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }
  }
}
