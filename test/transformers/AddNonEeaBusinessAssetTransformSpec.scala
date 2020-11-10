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
import utils.JsonUtils

class AddNonEeaBusinessAssetTransformSpec extends FreeSpec with MustMatchers {

  val nonEeaBusinessAsset = NonEEABusinessType(
    "1",
    "TestOrg",
  AddressType(
    "Line 1",
    "Line 2",
    None,
    None,
    Some("NE11NE"),
    "UK"),
    "UK",
    LocalDate.parse("2000-01-01"),
    None
  )

  "the add NonEeaBusinessAsset transformer should" - {

    "add a new NonEeaBusinessAsset when there are no NonEeaBusinessAssets existing" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-NonEeaBusinessAsset.json")

      val transformer = AddNonEeaBusinessAssetTransform(nonEeaBusinessAsset)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new NonEeaBusinessAsset when there are NonEeaBusinessAssets existing" in {

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-NonEeaBusinessAsset.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-NonEeaBusinessAsset.json")

      val transformer = AddNonEeaBusinessAssetTransform(nonEeaBusinessAsset)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}