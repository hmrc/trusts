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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.trusts.models.{NameType, PassportType}
import uk.gov.hmrc.trusts.models.variation.{IdentificationType, IndividualDetailsType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AmendIndividualBeneficiaryTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  "AmendIndividualBeneficiaryTransform should" - {

    "successfully update a beneficiary's details" in {

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-individual-beneficiary-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-individual-beneficiary-transform-after.json")

      val amended = IndividualDetailsType(
        lineNo = Some("2"),
        bpMatchStatus = Some("01"),
        NameType("First 2", None, "Last 2"),
        None,
        vulnerableBeneficiary = false,
        None,
        None,
        None,
        identification = Some(IdentificationType(
          nino = None,
          passport = Some(PassportType(
            number = "123456789",
            expirationDate = DateTime.parse("2025-01-01"),
            countryOfIssue = "DE"
          )),
          address = None,
          safeId = None
        )),
        DateTime.parse("2010-01-01"),
        None
      )

      val original: JsValue = Json.parse(
        """
          |{
          |  "lineNo": "2",
          |  "bpMatchStatus": "01",
          |  "name": {
          |    "firstName": "First 2",
          |    "lastName": "Last 2"
          |  },
          |  "vulnerableBeneficiary": true,
          |  "identification": {
          |    "nino": "JP1212122A"
          |  },
          |  "entityStart": "2018-02-28"
          |}
          |""".stripMargin)

      val transformer = AmendIndividualBeneficiaryTransform(1, amended, original)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }
  }

}
