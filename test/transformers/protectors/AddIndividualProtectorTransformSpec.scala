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

package transformers.protectors

import models.NameType
import models.variation.{IdentificationType, Protector}
import org.scalatest.{FreeSpec, MustMatchers}
import utils.JsonUtils

import java.time.LocalDate

class AddIndividualProtectorTransformSpec extends FreeSpec with MustMatchers {

  val newProtector = Protector(Some("1"),
    None,
    NameType("abcdefghijkl",Some("abcdefghijklmn"), "abcde"),
    Some(LocalDate.parse("2000-01-01")),
    Some(IdentificationType(Some("ST019091"),None,None, None)),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None,
    LocalDate.parse("2002-01-01"),
    None
  )

  val newSecondProtector = Protector(None,
    None,
    NameType("second",None, "protector"),
    Some(LocalDate.parse("2000-01-01")),
    Some(IdentificationType(Some("AB123456"),None,None, None)),
    countryOfResidence = None,
    legallyIncapable = None,
    nationality = None,
    LocalDate.parse("2010-01-01"),
    None
  )

  "the add individual protector transformer should" - {

    "add a new individual protector when there are no protector existing" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-no-protectors.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-individual-protector.json")

      val transformer = new AddIndividualProtectorTransform(newProtector)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new individual protector" in {

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-individual-protector.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-individual-protector.json")

      val transformer = new AddIndividualProtectorTransform(newSecondProtector)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}