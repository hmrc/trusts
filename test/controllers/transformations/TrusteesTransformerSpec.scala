/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.transformations

import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import transformers.mdtp.Trustees
import utils.JsonFixtures

class TrusteesTransformerSpec extends AnyWordSpec with JsonFixtures {

  // this is how ETMP should have the data saved
  val jsonGood: JsValue = Json.parse(
    """
      |{
      |  "details": {
      |     "trust":  {
      |        "entities": {
      |          "trustees": [
      |            {
      |              "trusteeInd": {
      |                "lineNo": "1",
      |                "bpMatchStatus": "01",
      |                "name": {
      |                  "firstName": "Tamara",
      |                  "middleName": "Hingis",
      |                  "lastName": "Jones"
      |                },
      |                "dateOfBirth": "1965-02-28",
      |                "identification": {
      |                  "nino": "AA000000A"
      |                },
      |                "phoneNumber": "+447456788112",
      |                "entityStart": "2017-02-28"
      |              }
      |            },
      |            {
      |              "trusteeOrg": {
      |                "lineNo": "newLineNo",
      |                "bpMatchStatus": "newMatchStatus",
      |                "name": "newName",
      |                "phoneNumber": "newPhone",
      |                "email": "newEmail",
      |                "identification": {
      |                  "utr": "newUtr"
      |                },
      |                "entityStart": "2017-02-28"
      |              }
      |            }
      |          ]
      |       }
      |     }
      |   }
      |}
      |""".stripMargin
  )

  // this is how ETMP sometimes has the data saved (investigation ongoing)
  val jsonBad: JsValue = Json.parse(
    """
      |{
      |  "details": {
      |     "trust":  {
      |        "entities": {
      |          "trustees": [
      |            {
      |              "trusteeInd": {
      |                "lineNo": "1",
      |                "bpMatchStatus": "01",
      |                "name": {
      |                  "firstName": "Tamara",
      |                  "middleName": "Hingis",
      |                  "lastName": "Jones"
      |                },
      |                "dateOfBirth": "1965-02-28",
      |                "identification": {
      |                  "nino": "AA000000A"
      |                },
      |                "phoneNumber": "+447456788112",
      |                "entityStart": "2017-02-28"
      |              }
      |            ,
      |
      |              "trusteeOrg": {
      |                "lineNo": "newLineNo",
      |                "bpMatchStatus": "newMatchStatus",
      |                "name": "newName",
      |                "phoneNumber": "newPhone",
      |                "email": "newEmail",
      |                "identification": {
      |                  "utr": "newUtr"
      |                },
      |                "entityStart": "2017-02-28"
      |              }
      |            }
      |          ]
      |       }
      |     }
      |   }
      |}
      |""".stripMargin
  )

  "Trustees" should {
    "handle good json with one trustee per array slot and bad json with two trustee per array slot" in {

      val a = jsonGood.transform(Trustees.transform(jsonGood)).get
      val b = jsonBad.transform(Trustees.transform(jsonBad)).get

      assert(a === b)

    }
  }

}
