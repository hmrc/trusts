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

package transformers.assets

import models.AddressType
import models.variation.{NonEEABusinessType, OtherAssetType}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class AmendAssetTransformSpec extends FreeSpec with MustMatchers {

  private val address =  AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "GB")

  private val originalAsset: NonEEABusinessType = NonEEABusinessType(
    lineNo = "2",
    orgName = "Original Name",
    address = address,
    govLawCountry = "GB",
    startDate = LocalDate.parse("2010-01-01"),
    endDate = None
  )

  private val amendedAsset: NonEEABusinessType = originalAsset.copy(orgName = "Amended Name")

  private val endDate = LocalDate.parse("2012-12-20")

  "the amend asset transformer should" - {

    "before declaration" - {

      "successfully update an asset's details" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-non-eea-business-asset-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-non-eea-business-asset-transform-after.json")

        val transformer = AmendAssetTransform(1, Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, amendedAsset.toString)

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "when non-EEA business asset" - {
        "set an end date for the original asset and add in the amendment as a new asset not known to ETMP" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-non-eea-business-asset-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-non-eea-business-asset-transform-after-declaration.json")

          val transformer = AmendAssetTransform(1, Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, amendedAsset.toString)

          val transformed = transformer.applyTransform(beforeJson).get

          val result = transformer.applyDeclarationTransform(transformed).get
          result mustBe afterJson
        }
      }

      "when not non-EEA business asset" - {
        "not set an end date for the original asset and amend the original asset known to ETMP" in {

          val originalAsset: OtherAssetType = OtherAssetType(
            description = "Original description 1",
            value = None
          )

          val amendedAsset: OtherAssetType = originalAsset.copy(description = "Amended description 1")

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-other-asset-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-other-asset-transform-after-declaration.json")

          val transformer = AmendAssetTransform(0, Json.toJson(amendedAsset), Json.toJson(originalAsset), endDate, amendedAsset.toString)

          val transformed = transformer.applyTransform(beforeJson).get

          val result = transformer.applyDeclarationTransform(transformed).get
          result mustBe afterJson
        }
      }
    }
  }
}
