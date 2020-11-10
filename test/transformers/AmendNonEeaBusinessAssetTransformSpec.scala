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

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import utils.JsonUtils

class AmendNonEeaBusinessAssetTransformSpec extends FreeSpec with MustMatchers {

  private val originalJson = Json.parse(
    """
      |{
      |  "lineNo":"1",
      |  "bpMatchStatus": "01",
      |  "orgName": "TestOrg",
      |  "Address": "Line 1, Line 2, NE11NE, UK",
      |  "govLawCountry": "UK",
      |  "startDate": "2000-01-01"
      |}
      |""".stripMargin)

  private val endDate = LocalDate.of(2012, 12, 20)

  "AmendNonEeaBusinessAssetTransform should" - {

    "before declaration" - {

      "successfully update a AmendNonEeaBusinessAsset's details" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-after.json")
        val newName = "TestOrg2"
        val transformer = AmendNonEeaBusinessAssetTransform(1, newName, originalJson, endDate)

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "set an end date for the original NonEeaBusinessAsset, adding in the amendment as a new NonEeaBusinessAsset for a NonEeaBusinessAsset known by etmp" in {
        val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-unidentified-beneficiary-transform-after-declaration.json")
        val newDescription = "This description has been updated"
        val transformer = AmendNonEeaBusinessAssetTransform(1, newDescription, originalJson, endDate)

        val transformed = transformer.applyTransform(beforeJson).get
        val result = transformer.applyDeclarationTransform(transformed).get
        result mustBe afterJson
      }
    }
  }
}
