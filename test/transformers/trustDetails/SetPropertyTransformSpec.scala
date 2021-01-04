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

package transformers.trustDetails

import org.scalatest.{FreeSpec, MustMatchers}
import utils.JsonUtils

class SetPropertyTransformSpec extends FreeSpec with MustMatchers {

  "SetPropertyTransform should" - {

    "before declaration" - {

      "amend an trust details by setting the field" - {
        "when the field already exists" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before-populated-property.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-property.json")

          val transformer = SetPropertyTransform(false)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }

        "when it is a new value" in {

          val beforeJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-before.json")
          val afterJson = JsonUtils.getJsonValueFromFile("transforms/trusts-details-transform-after-property.json")

          val transformer = SetPropertyTransform(false)

          val result = transformer.applyTransform(beforeJson).get
          result mustBe afterJson
        }


      }

    }

  }

}
