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

package models

import base.BaseSpec
import play.api.libs.json.Json

class BackwardsCompatibilitySpec extends BaseSpec {

  "BackwardsCompatibility" when {

    "agent details" must {

      val ref: String = "1234567890"
      val name: String = "Agency"
      val tel: String = "0191 00000000"
      val arn: String = "0987654321"
      val ukAddress: AddressType = AddressType("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), Some("AB1 1AB"), "GB")
      val nonUkAddress: AddressType = AddressType("Line 1", "Line 2", Some("Line 3"), None, None, "FR")

      "validate old-style" when {

        "uk address" in {

          val json = Json.parse(
            s"""
              |{
              |  "internalReference": "$ref",
              |  "name": "$name",
              |  "addressYesNo": true,
              |  "ukAddress": {
              |    "line1" : "Line 1",
              |    "line2" : "Line 2",
              |    "line3" : "Line 3",
              |    "line4" : "Line 4",
              |    "postcode" : "AB1 1AB"
              |  },
              |  "telephoneNumber": "$tel",
              |  "agentARN": "$arn"
              |}
              |""".stripMargin)

          val result = json.validate[AgentDetailsBC].get

          result mustBe AgentDetailsBC(
            internalReference = ref,
            name = name,
            addressUKYesNo = true,
            ukAddress = Some(ukAddress),
            internationalAddress = None,
            telephoneNumber = tel,
            agentARN = arn
          )
        }

        "non-uk address" in {

          val json = Json.parse(
            s"""
               |{
               |  "internalReference": "$ref",
               |  "name": "$name",
               |  "addressYesNo": false,
               |  "internationalAddress": {
               |    "line1" : "Line 1",
               |    "line2" : "Line 2",
               |    "line3" : "Line 3",
               |    "country" : "FR"
               |  },
               |  "telephoneNumber": "$tel",
               |  "agentARN": "$arn"
               |}
               |""".stripMargin)

          val result = json.validate[AgentDetailsBC].get

          result mustBe AgentDetailsBC(
            internalReference = ref,
            name = name,
            addressUKYesNo = false,
            ukAddress = None,
            internationalAddress = Some(nonUkAddress),
            telephoneNumber = tel,
            agentARN = arn
          )
        }
      }

      "write new-style" when {

        "uk address" in {

          val agency = AgentDetailsBC(
            internalReference = ref,
            name = name,
            addressUKYesNo = true,
            ukAddress = Some(ukAddress),
            internationalAddress = None,
            telephoneNumber = tel,
            agentARN = arn
          )

          val result = Json.toJson(agency)

          result mustBe Json.parse(
            s"""
               |{
               |  "internalReference": "$ref",
               |  "name": "$name",
               |  "addressUKYesNo": true,
               |  "ukAddress": {
               |    "line1" : "Line 1",
               |    "line2" : "Line 2",
               |    "line3" : "Line 3",
               |    "line4" : "Line 4",
               |    "postcode" : "AB1 1AB",
               |    "country": "GB"
               |  },
               |  "telephoneNumber": "$tel",
               |  "agentARN": "$arn"
               |}
               |""".stripMargin)
        }

        "non-uk address" in {

          val agency = AgentDetailsBC(
            internalReference = ref,
            name = name,
            addressUKYesNo = false,
            ukAddress = None,
            internationalAddress = Some(nonUkAddress),
            telephoneNumber = tel,
            agentARN = arn
          )

          val result = Json.toJson(agency)

          result mustBe Json.parse(
            s"""
               |{
               |  "internalReference": "$ref",
               |  "name": "$name",
               |  "addressUKYesNo": false,
               |  "internationalAddress": {
               |    "line1" : "Line 1",
               |    "line2" : "Line 2",
               |    "line3" : "Line 3",
               |    "country": "FR"
               |  },
               |  "telephoneNumber": "$tel",
               |  "agentARN": "$arn"
               |}
               |""".stripMargin)
        }
      }
    }
  }
}
