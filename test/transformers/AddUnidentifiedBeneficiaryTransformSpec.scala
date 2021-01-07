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
import models.variation.UnidentifiedType
import transformers.beneficiaries.AddUnidentifiedBeneficiaryTransform
import utils.JsonUtils

class AddUnidentifiedBeneficiaryTransformSpec extends FreeSpec with MustMatchers {

  val newBeneficiary = UnidentifiedType(None,
    None,
    "Some Description",
    None,
    None,
    LocalDate.parse("1990-10-10"),
    None
  )

  "the add unidentified beneficiary transformer should" - {

    "add a new unidentified beneficiary when there are no beneficiaries existing" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-unidentified-beneficiary.json")

      val transformer = new AddUnidentifiedBeneficiaryTransform(newBeneficiary)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new unidentified beneficiary" in {
      val newBeneficiary = UnidentifiedType(None,
        None,
        "Some other description",
        None,
        None,
        LocalDate.parse("2000-10-10"),
        None
      )
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-unidentified-beneficiary.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-unidentified-beneficiary.json")

      val transformer = new AddUnidentifiedBeneficiaryTransform(newBeneficiary)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}