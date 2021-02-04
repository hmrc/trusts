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

package services

import base.BaseSpec
import models.registration.RegistrationSubmissionDraft
import play.api.libs.json.JsValue
import utils.JsonUtils

import java.time.LocalDateTime

class BackwardsCompatibilityServiceSpec extends BaseSpec {

  "BackwardsCompatibilityService" must {

    val service = new BackwardsCompatibilityService()

    "map old-style registration data to new-style registration data" ignore {

      val draftId: String = "358df5dd-63e3-4cad-aa93-403c83af97cd"
      val internalId: String = "Int-d387bcea-3ca2-48ab-b6bc-3919a050414d"
      val createdAt: LocalDateTime = LocalDateTime.of(2021, 2, 3, 14, 0)
      val reference: String = "234425525"

      val oldData: JsValue = JsonUtils.getJsonValueFromFile("backwardscompatibility/old_assets_and_agents_data.json")
      val newData: JsValue = JsonUtils.getJsonValueFromFile("backwardscompatibility/new_assets_and_agents_data.json")

      val oldDraft = RegistrationSubmissionDraft(draftId, internalId, createdAt, oldData, Some(reference), Some(true))

      val result = service.adjustData(oldDraft)

      result mustBe newData
    }
  }
}
