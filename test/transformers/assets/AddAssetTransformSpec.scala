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
import models.variation.NonEEABusinessType
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import utils.JsonUtils

import java.time.LocalDate

class AddAssetTransformSpec extends FreeSpec with MustMatchers {

  val asset: NonEEABusinessType = NonEEABusinessType(
    Some("1"),
    "TestOrg",
    AddressType("Line 1", "Line 2", None, None, Some("NE11NE"), "UK"),
    "UK",
    LocalDate.parse("2000-01-01"),
    None
  )

  val assetType: String = "nonEEABusiness"

  "the add asset transformer should" - {

    "add a new asset when there are no assets existing of the same type" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-NonEeaBusinessAsset.json")

      val transformer = AddAssetTransform(Json.toJson(asset), assetType)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new asset when there are assets existing of the same type" in {

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-NonEeaBusinessAsset.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-NonEeaBusinessAsset.json")

      val transformer = AddAssetTransform(Json.toJson(asset), assetType)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}