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

package uk.gov.hmrc.trusts.utils

import play.api.libs.json.JsValue
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.utils.JsonOps._

class JsonOpsSpec extends BaseSpec {

  "return modified json with no brackets in phone numbers" in {

    val initialJson: JsValue = JsonUtils.getJsonValueFromFile("data-with-brackets-in-phone-numbers.json")
    val reformattedJson: JsValue = JsonUtils.getJsonValueFromFile("data-with-reformatted-phone-numbers.json")

    initialJson.applyRules mustBe reformattedJson

  }

  "return modified json with no brackets in agent telephone numbers" in {

    val initialJson: JsValue = JsonUtils.getJsonValueFromFile("data-with-brackets-in-agent-telephone-numbers.json")
    val reformattedJson: JsValue = JsonUtils.getJsonValueFromFile("data-with-reformatted-agent-telephone-numbers.json")

    initialJson.applyRules mustBe reformattedJson

  }

}
