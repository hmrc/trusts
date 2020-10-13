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
import uk.gov.hmrc.trusts.models.variation.{BeneficiaryTrustType, IdentificationOrgType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddTrustBeneficiaryTransformSpec extends FreeSpec with MustMatchers {

  val newBeneficiary = BeneficiaryTrustType(
    None,
    None,
    "Organisation Name",
    Some(false),
    Some("50"),
    Some(IdentificationOrgType(
      Some("company utr"),
      Some(AddressType("Line 1", "Line 2", None, None, Some("NE1 1NE"), "GB")),
      None)),
    None,
    LocalDate.parse("1990-10-10"),
    None
  )

  "the add trust beneficiary transformer should" - {

    "add a new trust beneficiary" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-only-other-beneficiary.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-trust-beneficiary.json")

      val transformer = new AddTrustBeneficiaryTransform(newBeneficiary)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }
  }
}