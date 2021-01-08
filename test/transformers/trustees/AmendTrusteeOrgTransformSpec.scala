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

import models.variation.{IdentificationOrgType, TrusteeOrgType}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class AmendTrusteeOrgTransformSpec extends FreeSpec with MustMatchers {
  "the modify transformer should" - {
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

      val transformer = AmendTrusteeOrgTransform(
        1,
        newTrusteeInfo,
        originalTrusteeInfo,
        LocalDate.of(2012, 2, 20))

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }
  }
}