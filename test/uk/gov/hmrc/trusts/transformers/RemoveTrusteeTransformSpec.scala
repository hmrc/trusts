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

    "remove an already known individual trustee" in {

      val endDate = DateTime.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-remove-trustee-ind.json")

      val transformer = new RemoveTrusteeTransform(endDate = endDate, index = 0)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "remove a newly added individual trustee" in {

      val endDate = DateTime.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-newly-added-trustee.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val transformer = new RemoveTrusteeTransform(endDate, index = 0)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson

    }

    "remove a newly added organisation trustee" in {

      val endDate = DateTime.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-org-cached-newly-added.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-remove-trustee-ind.json")

      val transformer = new RemoveTrusteeTransform(endDate = endDate, index = 0)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "remove an already known organisation trustee" in {

      val endDate = DateTime.parse("2010-10-15")

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-org-cached.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-remove-trustee-ind.json")

      val transformer = new RemoveTrusteeTransform(endDate = endDate, index = 0)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }
  }

}
