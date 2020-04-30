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
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustTrusteeIndividualType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AmendTrusteeIndTransformSpec extends FreeSpec with MustMatchers  {

  "the modify transformer should" - {

    "successfully set a new trustee's details" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-trustee-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-trustee-transform-after-ind.json")
      val newTrusteeInfo = DisplayTrustTrusteeIndividualType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = Some(LocalDate.of(1965, 2, 10)),
        phoneNumber = Some("newPhone"),
        identification = Some(DisplayTrustIdentificationType(None, Some("newNino"), None, None)),
        entityStart = LocalDate.of(2019, 2, 10)
      )

      val originalTrusteeInfo = Json.parse(
        """
          |{
          |   "trusteeInd":{
          |     "lineNo": "1",
          |     "bpMatchStatus": "01",
          |     "name":{
          |       "firstName":"Tamara",
          |       "middleName":"Hingis",
          |       "lastName":"Jones"
          |     },
          |     "dateOfBirth":"1956-02-28",
          |     "identification":{
          |       "nino":"AA000000A"
          |     },
          |     "entityStart":"2017-02-28"
          |   }
          |}
          |""".stripMargin)

      val transformer = AmendTrusteeIndTransform(
        0,
        newTrusteeInfo,
        originalTrusteeInfo,
        LocalDate.of(2012, 2, 20))

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }
  }
}
