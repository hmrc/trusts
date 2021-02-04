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

    val name: String = "Name"
    val nonUkAddress: AddressType = AddressType("Line 1", "Line 2", Some("Line 3"), None, None, "FR")

    "agent details" must {

      val ref: String = "1234567890"
      val tel: String = "0191 00000000"
      val arn: String = "0987654321"
      val ukAddress: AddressType = AddressType("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), Some("AB1 1AB"), "GB")

      "validate old style" when {

        "uk address" in {

          val json = Json.parse(
            s"""
              |{
              |  "internalReference": "$ref",
              |  "name": "$name",
              |  "addressYesNo": true,
              |  "ukAddress": {
              |    "line1": "Line 1",
              |    "line2": "Line 2",
              |    "line3": "Line 3",
              |    "line4": "Line 4",
              |    "postcode": "AB1 1AB"
              |  },
              |  "telephoneNumber": "$tel",
              |  "agentARN": "$arn"
              |}
              |""".stripMargin)

          val result = json.validate[AgentDetailsBC].get

          result mustBe AgentDetailsBC(
            internalReference = ref,
            name = name,
            hasUkAddress = true,
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
               |    "line1": "Line 1",
               |    "line2": "Line 2",
               |    "line3": "Line 3",
               |    "country": "FR"
               |  },
               |  "telephoneNumber": "$tel",
               |  "agentARN": "$arn"
               |}
               |""".stripMargin)

          val result = json.validate[AgentDetailsBC].get

          result mustBe AgentDetailsBC(
            internalReference = ref,
            name = name,
            hasUkAddress = false,
            ukAddress = None,
            internationalAddress = Some(nonUkAddress),
            telephoneNumber = tel,
            agentARN = arn
          )
        }
      }

      "write new style" when {

        "uk address" in {

          val agency = AgentDetailsBC(
            internalReference = ref,
            name = name,
            hasUkAddress = true,
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
               |    "line1": "Line 1",
               |    "line2": "Line 2",
               |    "line3": "Line 3",
               |    "line4": "Line 4",
               |    "postcode": "AB1 1AB",
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
            hasUkAddress = false,
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
               |    "line1": "Line 1",
               |    "line2": "Line 2",
               |    "line3": "Line 3",
               |    "country": "FR"
               |  },
               |  "telephoneNumber": "$tel",
               |  "agentARN": "$arn"
               |}
               |""".stripMargin)
        }
      }
    }

    "assets" when {

      val status: String = "completed"
      val value: Long  = 4000L
      val quantity: Long  = 100L
      val description: String = "Description"

      "money" must {

        val `type`: String = "Money"

        val asset = MoneyAssetBC(
          value = value,
          `type` = `type`,
          status = status
        )

        "validate old style" in {

          val json = Json.parse(
            s"""
               |{
               |  "assetMoneyValue": "$value",
               |  "whatKindOfAsset": "${`type`}",
               |  "status": "$status"
               |}
               |""".stripMargin)

          val result = json.validate[MoneyAssetBC].get

          result mustBe asset
        }

        "write new style" in {

          val result = Json.toJson(asset)

          result mustBe Json.parse(
            s"""
               |{
               |  "moneyValue": $value,
               |  "whatKindOfAsset": "${`type`}",
               |  "status": "$status"
               |}
               |""".stripMargin)
        }
      }

      "shares" must {

        val `type`: String = "Shares"

        val shareClass: String = "ordinary"

        val shareInPortfolio = SharesAssetBC(
          sharesInPortfolio = true,
          portfolioSharesName = Some(name),
          portfolioSharesOnStockExchangeYesNo = Some(true),
          portfolioSharesQuantity = Some(quantity),
          portfolioSharesValue = Some(value),
          nonPortfolioSharesName = None,
          nonPortfolioSharesOnStockExchangeYesNo = None,
          nonPortfolioSharesClass = None,
          nonPortfolioSharesQuantity = None,
          nonPortfolioSharesValue = None,
          `type` = `type`,
          status = status
        )

        val shareNotInPortfolio = SharesAssetBC(
          sharesInPortfolio = false,
          portfolioSharesName = None,
          portfolioSharesOnStockExchangeYesNo = None,
          portfolioSharesQuantity = None,
          portfolioSharesValue = None,
          nonPortfolioSharesName = Some(name),
          nonPortfolioSharesOnStockExchangeYesNo = Some(false),
          nonPortfolioSharesClass = Some(shareClass),
          nonPortfolioSharesQuantity = Some(quantity),
          nonPortfolioSharesValue = Some(value),
          `type` = `type`,
          status = status
        )

        "validate old style" when {

          "in a portfolio" in {

            val json = Json.parse(
              s"""
                 |{
                 |  "portfolioQuantityInTheTrust": "$quantity",
                 |  "sharesInAPortfolio": true,
                 |  "name": "$name",
                 |  "portfolioListedOnTheStockExchange": true,
                 |  "portfolioValue": "$value",
                 |  "whatKindOfAsset": "${`type`}",
                 |  "status": "$status"
                 |}
                 |""".stripMargin)

            val result = json.validate[SharesAssetBC].get

            result mustBe shareInPortfolio
          }

          "not in a portfolio" in {

            val json = Json.parse(
              s"""
                 |{
                 |  "listedOnTheStockExchange": false,
                 |  "shareCompanyName": "$name",
                 |  "sharesInAPortfolio": false,
                 |  "class": "$shareClass",
                 |  "quantityInTheTrust": "$quantity",
                 |  "value": "$value",
                 |  "whatKindOfAsset": "${`type`}",
                 |  "status": "$status"
                 |}
                 |""".stripMargin)

            val result = json.validate[SharesAssetBC].get

            result mustBe shareNotInPortfolio
          }
        }

        "write new style" when {

          "in a portfolio" in {

            val result = Json.toJson(shareInPortfolio)

            result mustBe Json.parse(
              s"""
                 |{
                 |  "sharesInPortfolioYesNo": true,
                 |  "portfolioSharesName": "$name",
                 |  "portfolioSharesOnStockExchangeYesNo": true,
                 |  "portfolioSharesValue": $value,
                 |  "portfolioSharesQuantity": $quantity,
                 |  "whatKindOfAsset": "${`type`}",
                 |  "status": "$status"
                 |}
                 |""".stripMargin)
          }

          "not in a portfolio" in {

            val result = Json.toJson(shareNotInPortfolio)

            result mustBe Json.parse(
              s"""
                 |{
                 |  "sharesInPortfolioYesNo": false,
                 |  "nonPortfolioSharesName": "$name",
                 |  "nonPortfolioSharesOnStockExchangeYesNo": false,
                 |  "nonPortfolioSharesClass": "$shareClass",
                 |  "nonPortfolioSharesValue": $value,
                 |  "nonPortfolioSharesQuantity": $quantity,
                 |  "whatKindOfAsset": "${`type`}",
                 |  "status": "$status"
                 |}
                 |""".stripMargin)
          }
        }
      }

      "business" must {

        val `type`: String = "Business"

        val ukAddress: AddressTypeNoCountry = AddressTypeNoCountry("Line 1", "Line 2", Some("Line 3"), Some("Line 4"), Some("AB1 1AB"))

        val assetUk = BusinessAssetBC(
          name = name,
          description = description,
          hasUkAddress = true,
          ukAddress = Some(ukAddress),
          internationalAddress = None,
          value = value,
          `type` = `type`,
          status = status
        )

        val assetNonUk = BusinessAssetBC(
          name = name,
          description = description,
          hasUkAddress = false,
          ukAddress = None,
          internationalAddress = Some(nonUkAddress),
          value = value,
          `type` = `type`,
          status = status
        )

        "validate old style" when {

          "uk address" in {

            val json = Json.parse(
              s"""
                 |{
                 |  "name": "$name",
                 |  "description": "$description",
                 |  "addressUkYesNo": true,
                 |  "ukAddress": {
                 |    "line1": "Line 1",
                 |    "line2": "Line 2",
                 |    "line3": "Line 3",
                 |    "line4": "Line 4",
                 |    "postcode": "AB1 1AB"
                 |  },
                 |  "value": "$value",
                 |  "whatKindOfAsset": "${`type`}",
                 |  "status": "$status"
                 |}
                 |""".stripMargin)

            val result = json.validate[BusinessAssetBC].get

            result mustBe assetUk
          }

          "non-uk address" in {

            val json = Json.parse(
              s"""
                 |{
                 |  "name": "$name",
                 |  "description": "$description",
                 |  "addressUkYesNo": false,
                 |  "internationalAddress": {
                 |    "line1": "Line 1",
                 |    "line2": "Line 2",
                 |    "line3": "Line 3",
                 |    "country": "FR"
                 |  },
                 |  "value": "$value",
                 |  "whatKindOfAsset": "${`type`}",
                 |  "status": "$status"
                 |}
                 |""".stripMargin)

            val result = json.validate[BusinessAssetBC].get

            result mustBe assetNonUk
          }
        }

        "write new style" when {

          "uk address" in {

            val result = Json.toJson(assetUk)

            result mustBe Json.parse(
              s"""
                 |{
                 |  "businessName": "$name",
                 |  "businessDescription": "$description",
                 |  "businessAddressUkYesNo": true,
                 |  "businessUkAddress": {
                 |    "line1": "Line 1",
                 |    "line2": "Line 2",
                 |    "line3": "Line 3",
                 |    "line4": "Line 4",
                 |    "postcode": "AB1 1AB"
                 |  },
                 |  "businessValue": $value,
                 |  "whatKindOfAsset": "${`type`}",
                 |  "status": "$status"
                 |}
                 |""".stripMargin)
          }

          "non-uk address" in {

            val result = Json.toJson(assetNonUk)

            result mustBe Json.parse(
              s"""
                 |{
                 |  "businessName": "$name",
                 |  "businessDescription": "$description",
                 |  "businessAddressUkYesNo": false,
                 |  "businessInternationalAddress": {
                 |    "line1": "Line 1",
                 |    "line2": "Line 2",
                 |    "line3": "Line 3",
                 |    "country": "FR"
                 |  },
                 |  "businessValue": $value,
                 |  "whatKindOfAsset": "${`type`}",
                 |  "status": "$status"
                 |}
                 |""".stripMargin)
          }
        }
      }

      "other" must {

        val `type`: String = "Other"

        val asset = OtherAssetBC(
          description = description,
          value = value,
          `type` = `type`,
          status = status
        )

        "validate old style" in {

          val json = Json.parse(
            s"""
               |{
               |  "otherAssetDescription": "$description",
               |  "otherAssetValue": "$value",
               |  "whatKindOfAsset": "${`type`}",
               |  "status": "$status"
               |}
               |""".stripMargin)

          val result = json.validate[OtherAssetBC].get

          result mustBe asset
        }

        "write new style" in {

          val result = Json.toJson(asset)

          result mustBe Json.parse(
            s"""
               |{
               |  "otherDescription": "$description",
               |  "otherValue": $value,
               |  "whatKindOfAsset": "${`type`}",
               |  "status": "$status"
               |}
               |""".stripMargin)
        }
      }
    }
  }
}
