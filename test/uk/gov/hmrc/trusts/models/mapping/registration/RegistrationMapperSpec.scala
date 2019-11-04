/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.models.mapping.registration

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.models.Trust
import uk.gov.hmrc.trusts.utils.DataExamples


class RegistrationMapperSpec extends BaseSpec with DataExamples {

  "Registration" should {
    "map trust to des representation of trust" in {
      val apiRegistration = registrationRequest
      val desRegistration: JsValue = Json.toJson(apiRegistration)
       (desRegistration \ "details" \ "trust").get.as[Trust].details mustBe apiRegistration.trust.details
    }
  }
}
