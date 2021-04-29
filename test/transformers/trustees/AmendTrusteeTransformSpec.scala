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

package transformers.trustees

import models.NameType
import models.variation._
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class AmendTrusteeTransformSpec extends FreeSpec with MustMatchers  {

  "the amend trustee transformer when" - {

    val endDate: LocalDate = LocalDate.of(2012, 2, 20)

    "individual trustee should" - {

      "successfully set a new trustee's details" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-trustee-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-trustee-transform-after-ind.json")

        val newTrusteeInfo = TrusteeIndividualType(
          lineNo = Some("newLineNo"),
          bpMatchStatus = Some("newMatchStatus"),
          name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
          dateOfBirth = Some(LocalDate.of(1965, 2, 10)),
          phoneNumber = Some("newPhone"),
          identification = Some(IdentificationType(Some("newNino"), None, None, None)),
          countryOfResidence = None,
          legallyIncapable = None,
          nationality = None,
          entityStart = LocalDate.of(2019, 2, 10),
          entityEnd = None
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

        val transformer = AmendTrusteeTransform(Some(0), Json.toJson(newTrusteeInfo), originalTrusteeInfo, endDate, "trusteeInd")

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "business trustee should" - {

      "successfully set a new trustee's details" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-trustee-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-trustee-transform-after-org.json")

        val newTrusteeInfo = TrusteeOrgType(
          lineNo = Some("newLineNo"),
          bpMatchStatus = Some("newMatchStatus"),
          name = "newName",
          phoneNumber = Some("newPhone"),
          email = Some("newEmail"),
          identification = Some(IdentificationOrgType(Some("newUtr"), None, None)),
          countryOfResidence = None,
          entityStart = LocalDate.of(2019, 2, 10),
          entityEnd = None
        )

        val originalTrusteeInfo = Json.parse(
          """
            |{
            |   "trusteeOrg":{
            |     "lineNo": "1",
            |     "bpMatchStatus": "01",
            |     "name": "MyOrg Incorporated",
            |     "phoneNumber": "+447456788112",
            |     "identification":{
            |       "utr": "1234567890"
            |     },
            |     "entityStart": "2017-02-28"
            |   }
            |}
            |""".stripMargin)

        val transformer = AmendTrusteeTransform(Some(1), Json.toJson(newTrusteeInfo), originalTrusteeInfo, endDate, "trusteeOrg")

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "individual lead trustee when" - {

      "not fully matched should" - {
        "successfully set a new ind lead trustee's details" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-after-ind.json")

          val newTrusteeInfo = AmendedLeadTrusteeIndType(
            bpMatchStatus = None,
            name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
            dateOfBirth = LocalDate.of(1965, 2, 10),
            phoneNumber = "newPhone",
            email = Some("newEmail"),
            identification = IdentificationType(Some("newNino"), None, None, None),
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None
          )

          val transformer = AmendTrusteeTransform(None, Json.toJson(newTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }

      "fully matched should" - {
        "successfully set a new ind lead trustee's details" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-lead-trustee-transform-after-ind-fully-matched.json")

          val newTrusteeInfo = AmendedLeadTrusteeIndType(
            bpMatchStatus = Some("01"),
            name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
            dateOfBirth = LocalDate.of(1965, 2, 10),
            phoneNumber = "newPhone",
            email = Some("newEmail"),
            identification = IdentificationType(Some("newNino"), None, None, None),
            countryOfResidence = None,
            legallyIncapable = None,
            nationality = None
          )

          val transformer = AmendTrusteeTransform(None, Json.toJson(newTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeInd")

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }
      }
    }

    "business lead trustee should" - {

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

        val transformer = AmendTrusteeTransform(None, Json.toJson(newTrusteeInfo), Json.obj(), LocalDate.now(), "leadTrusteeOrg")

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }
  }
}
