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

package uk.gov.hmrc.trusts.transformers

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.trusts.utils.JsonUtils

class RemoveTrusteeTransformSpec extends FreeSpec with MustMatchers with OptionValues  {

  "the remove trustee transformer must" - {

    "remove a trustee at the head of the list" in {

      val endDate = DateTime.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-remove-trustee-ind.json")

      val transformer = new RemoveTrusteeTransform(endDate = endDate, index = 0)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "remove a trustee at the tail of the list" in {

      val endDate = DateTime.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-newly-added-trustee.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-transformed-trustee-at-tail-removed.json")

      val transformer = new RemoveTrusteeTransform(endDate, index = 1)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "remove a trustee at an index and persist the remaining trustees" in {

      val endDate = DateTime.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-transformed-trustee-removed-at-index.json")

      val transformer = new RemoveTrusteeTransform(endDate, index = 1)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "do not remove any trustees when removing an index which is index out of bounds" in {
      val endDate = DateTime.parse("2010-10-15")

      // 4 trustees in the list, last index is 3
      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformer = new RemoveTrusteeTransform(endDate, index = 4)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe cachedJson
    }

    "remove multiple trustees in a sequence of transformations (shrinking list)" in {

      val endDate = DateTime.parse("2010-10-15")

      // 4 trustees in the list, last index is 3
      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-multiple-trustees-removed.json")

      // remove trustee individual
      val firstTrusteeRemoved = new RemoveTrusteeTransform(endDate, index = 0).applyTransform(cachedJson).get

      // remove trutee organisation
      val result = new RemoveTrusteeTransform(endDate, 1).applyTransform(firstTrusteeRemoved).get

      result mustBe transformedJson

    }

  }

}
