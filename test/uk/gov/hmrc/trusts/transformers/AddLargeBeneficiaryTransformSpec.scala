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
import uk.gov.hmrc.trusts.models.AddressType
import uk.gov.hmrc.trusts.models.variation.{IdentificationOrgType, LargeType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddLargeBeneficiaryTransformSpec extends FreeSpec with MustMatchers {

  val newBeneficiary = LargeType(
    None,
    None,
    "Name",
    "Description",
    None,
    None,
    None,
    None,
    "501",
    Some(IdentificationOrgType(
      None,
      Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
      None
    )),
    None,
    None,
    None,
    LocalDate.parse("2010-01-01"),
    None
  )

  "the add large beneficiary transformer should" - {

    "add a new large beneficiary" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-large-beneficiary.json")

      val transformer = new AddLargeBeneficiaryTransform(newBeneficiary)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }
  }
}