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

package transformers

import java.time.LocalDate

import models.AddressType
import models.variation.NonEEABusinessType
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import utils.JsonUtils

class AmendNonEeaBusinessAssetTransformSpec extends FreeSpec with MustMatchers {

  private val address =  AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "GB")

  private val originalNonEeaBusinessAsset: NonEEABusinessType = NonEEABusinessType(
    lineNo = "2",
    orgName = "Original Name",
    address = address,
    govLawCountry = "GB",
    startDate = LocalDate.parse("2010-01-01"),
    endDate = None
  )

  private val amendedNonEeaBusinessAsset: NonEEABusinessType = originalNonEeaBusinessAsset.copy(orgName = "Amended Name")

  private val endDate = LocalDate.parse("2012-12-20")

  "AmendNonEeaBusinessAssetTransform should" - {

    "before declaration" - {

      "successfully update a NonEeaBusinessAsset's details" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-non-eea-business-asset-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-non-eea-business-asset-transform-after.json")

        val transformer = AmendNonEeaBusinessAssetTransform(1, Json.toJson(amendedNonEeaBusinessAsset), Json.toJson(originalNonEeaBusinessAsset), endDate)

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "set an end date for the original NonEeaBusinessAsset, adding in the amendment as a new NonEeaBusinessAsset for a NonEeaBusinessAsset known by etmp" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-non-eea-business-asset-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-non-eea-business-asset-transform-after-declaration.json")

        val transformer = AmendNonEeaBusinessAssetTransform(1, Json.toJson(amendedNonEeaBusinessAsset), Json.toJson(originalNonEeaBusinessAsset), endDate)

        val transformed = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(transformed).get
        result mustBe afterJson
      }
    }
  }
}
