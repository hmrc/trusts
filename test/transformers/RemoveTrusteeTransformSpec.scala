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

package transformers

import java.time.LocalDate
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json._
import transformers.trustees.RemoveTrusteeTransform
import utils.JsonUtils

class RemoveTrusteeTransformSpec extends FreeSpec with MustMatchers  {

  private val originalTrusteeJson = Json.obj("field1" -> "value1", "field2" -> "value2")

  "the remove trustee transformer must" - {

    "remove a trustee at the head of the list" in {

      val endDate = LocalDate.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-remove-trustee-ind.json")

      val transformer = new RemoveTrusteeTransform(endDate = endDate, index = 0, originalTrusteeJson)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "remove a trustee at the tail of the list" in {

      val endDate = LocalDate.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-newly-added-trustee.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-transformed-trustee-at-tail-removed.json")

      val transformer = new RemoveTrusteeTransform(endDate, index = 1, originalTrusteeJson)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "remove a trustee at an index and persist the remaining trustees" in {

      val endDate = LocalDate.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-transformed-trustee-removed-at-index.json")

      val trustee = Json.parse(
        """
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

      val transformer = new RemoveTrusteeTransform(endDate, index = 2, trustee)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "don't remove any trustees when removing an index which is index out of bounds" in {
      val endDate = LocalDate.parse("2010-10-15")

      // 4 trustees in the list, last index is 3
      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformer = new RemoveTrusteeTransform(endDate, index = 4, originalTrusteeJson)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe cachedJson
    }

    "remove multiple trustees in a sequence of transformations (shrinking list)" in {

      val endDate = LocalDate.parse("2010-10-15")

      // 4 trustees in the list, last index is 3
      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees-removed.json")

      // remove trustee individual
      val firstTrusteeRemoved = new RemoveTrusteeTransform(endDate, index = 0, originalTrusteeJson).applyTransform(cachedJson).get

      // remove trustee organisation
      val result = new RemoveTrusteeTransform(endDate, 1, originalTrusteeJson).applyTransform(firstTrusteeRemoved).get

      result mustBe transformedJson

    }

    "re-add the original trustee, setting an end date at declaration, if known by Etmp" in {
      val removeKnownTrusteeToEtmp = Json.parse(
        """
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

      val removeNewlyAddedTrustee = Json.parse(
        """
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

      val transform1 = new RemoveTrusteeTransform(endDate, 1, removeKnownTrusteeToEtmp)
      val transform2 = new RemoveTrusteeTransform(endDate, 1, removeNewlyAddedTrustee)

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
