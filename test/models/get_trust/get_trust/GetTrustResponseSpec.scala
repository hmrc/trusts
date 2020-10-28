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

package models.get_trust.get_trust

import base.BaseSpec
import play.api.libs.json.Json
import models.Taxability._
import models.get_trust.get_trust

class GetTrustResponseSpec extends BaseSpec {

  private val responseHeader: ResponseHeader = ResponseHeader("status", "formBundleNo")

  "GetTrustResponse" when {

    "TrustProcessedResponse" must {

      "correctly identify the taxability of the trust" when {

        "taxable" in {

          val trust = Json.parse(
            """
              |{
              |  "matchData": {
              |    "utr": "1234567890"
              |  }
              |}
              |""".stripMargin
          )

          val response = TrustProcessedResponse(trust, responseHeader)

          response.taxability mustBe Taxable
        }

        "non-taxable" in {

          val trust = Json.parse(
            """
              |{
              |  "matchData": {
              |    "urn": "1234567890ABCDE"
              |  }
              |}
              |""".stripMargin
          )

          val response = get_trust.TrustProcessedResponse(trust, responseHeader)

          response.taxability mustBe NonTaxable
        }

        "converted from non-taxable to taxable" in {

          val trust = Json.parse(
            """
              |{
              |  "matchData": {
              |    "utr": "1234567890",
              |    "urn": "1234567890ABCDE"
              |  }
              |}
              |""".stripMargin
          )

          val response = get_trust.TrustProcessedResponse(trust, responseHeader)

          response.taxability mustBe ConvertedFromNonTaxableToTaxable
        }
      }

    }

  }

}
