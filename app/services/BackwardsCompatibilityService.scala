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

import models.registration.RegistrationSubmissionDraft
import play.api.Logging
import play.api.libs.json._
import utils.JsonOps.{doNothing, putNewValue}

class BackwardsCompatibilityService extends Logging {

  def adjustData(draft: RegistrationSubmissionDraft): JsValue = {

    val reads = adjustAssets(draft) andThen adjustAgentDetails(draft)

    draft.draftData.transform(reads) match {
      case JsSuccess(updatedData, _) =>
        updatedData
      case JsError(errors) =>
        logger.warn(s"Failed to adjust data: $errors")
        draft.draftData
    }
  }

  private def adjustAssets(draft: RegistrationSubmissionDraft): Reads[JsObject] = {
    adjust[JsArray](draft, "assets", "assets")
  }

  private def adjustAgentDetails(draft: RegistrationSubmissionDraft): Reads[JsObject] = {
    adjust[JsValue](draft, "agent", "agentDetails")
  }

  private def adjust[T <: JsValue](draft: RegistrationSubmissionDraft, oldKey: String, newKey: String)
                                  (implicit rds: Reads[T], wts: Writes[T]): Reads[JsObject] = {
    val oldPath: JsPath = __ \ "draftData" \ "main" \ "data" \ oldKey
    draft.draftData.transform(oldPath.json.pick[T]) match {
      case JsSuccess(value, _) =>
        val newPath: JsPath = __ \ "draftData" \ newKey
        val newObj = Json.obj(
          "_id" -> draft.draftId,
          "data" -> Json.obj(
            s"$oldKey" -> value
          ),
          "internalId" -> draft.internalId
        )
        putNewValue(newPath, newObj)
      case _ =>
        doNothing()
    }
  }
}
