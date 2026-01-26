/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers._
import play.api.libs.json._
import utils.JsonUtils

import java.time.LocalDate

class RemoveTrusteeTransformSpec extends AnyFreeSpec {

  private val originalTrusteeJson = Json.obj("field1" -> "value1", "field2" -> "value2")

  "the remove trustee transformer must" - {

    "remove a trustee at the head of the list" in {

      val endDate = LocalDate.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-remove-trustee-ind.json")

      val transformer =
        new RemoveTrusteeTransform(index = Some(0), originalTrusteeJson, endDate = endDate, "trusteeInd")

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "remove a trustee at the tail of the list" in {

      val endDate = LocalDate.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-newly-added-trustee.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-transformed-trustee-at-tail-removed.json")

      val transformer = new RemoveTrusteeTransform(index = Some(1), originalTrusteeJson, endDate, "trusteeInd")

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "remove a trustee at an index and persist the remaining trustees" in {

      val endDate = LocalDate.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-transformed-trustee-removed-at-index.json")

      val trustee = Json.parse("""
          |          {
          |            "trusteeOrg": {
          |              "name": "Trustee Org 1",
          |              "phoneNumber": "0121546546",
          |              "identification": {
          |                "utr": "5465416546"
          |              },
          |              "entityStart":"1998-02-12"
          |            }
          |          }
          |""".stripMargin)

      val transformer = new RemoveTrusteeTransform(index = Some(2), trustee, endDate, "trusteeInd")

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "don't remove any trustees when removing an index which is index out of bounds" in {
      val endDate = LocalDate.parse("2010-10-15")

      // 4 trustees in the list, last index is 3
      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformer = new RemoveTrusteeTransform(index = Some(4), originalTrusteeJson, endDate, "trusteeInd")

      val result = transformer.applyTransform(cachedJson).get

      result mustBe cachedJson
    }

    "remove multiple trustees in a sequence of transformations (shrinking list)" in {

      val endDate = LocalDate.parse("2010-10-15")

      // 4 trustees in the list, last index is 3
      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees-removed.json")

      // remove trustee individual
      val firstTrusteeRemoved = new RemoveTrusteeTransform(index = Some(0), originalTrusteeJson, endDate, "trusteeInd")
        .applyTransform(cachedJson)
        .get

      // remove trustee organisation
      val result = new RemoveTrusteeTransform(Some(1), originalTrusteeJson, endDate, "trusteeOrg")
        .applyTransform(firstTrusteeRemoved)
        .get

      result mustBe transformedJson

    }

    "re-add the original trustee, setting an end date at declaration, if known by Etmp" in {
      val removeKnownTrusteeToEtmp = Json.parse("""
          |          {
          |            "trusteeInd":{
          |              "lineNo":"1",
          |              "bpMatchStatus": "01",
          |              "name":{
          |                "firstName":"John",
          |                "middleName":"William",
          |                "lastName":"O'Connor"
          |              },
          |              "dateOfBirth":"1956-02-12",
          |              "identification":{
          |                "nino":"ST123456"
          |              },
          |              "phoneNumber":"0121546546",
          |              "entityStart":"1998-02-12"
          |            }
          |          }
          |""".stripMargin)

      val removeNewlyAddedTrustee = Json.parse("""
          |          {
          |            "trusteeOrg": {
          |              "name": "Trustee Org 1",
          |              "phoneNumber": "0121546546",
          |              "identification": {
          |                "utr": "5465416546"
          |              },
          |              "entityStart":"1998-02-12"
          |            }
          |          }
          |""".stripMargin)

      val endDate = LocalDate.parse("2010-10-15")

      val cachedEtmp = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees-removed-declared.json")

      val transform1 = new RemoveTrusteeTransform(Some(1), removeKnownTrusteeToEtmp, endDate, "trusteeInd")
      val transform2 = new RemoveTrusteeTransform(Some(1), removeNewlyAddedTrustee, endDate, "trusteeOrg")

      val declaredResult = for {
        result <- transform1.applyTransform(cachedEtmp)
        result <- transform2.applyTransform(result)
        result <- transform1.applyDeclarationTransform(result)
        result <- transform2.applyDeclarationTransform(result)
      } yield result

      declaredResult.get mustBe transformedJson
    }

  }

}
