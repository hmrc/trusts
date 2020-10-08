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

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.trusts.models.IdentificationOrgType
import uk.gov.hmrc.trusts.models.variation.SettlorCompany
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddBusinessSettlorTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  val newCompanySettlor = SettlorCompany(Some("1"),
    None,
    "Test",
    None,
    Some(false),
    Some(IdentificationOrgType(Some("ST019091"), None)),
    LocalDate.parse("2002-01-01"),
    None
  )

  "the add business settlor transformer should" - {

    "add a new business settlor when there are no settlor existing" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-no-business-settlor.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-add-new-business-settlor.json")

      val transformer = new AddBusinessSettlorTransform(newCompanySettlor)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new business settlor" in {

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-add-new-business-settlor.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-add-second-business-settlor.json")

      val transformer = new AddBusinessSettlorTransform(newCompanySettlor)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}