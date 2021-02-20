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

import models._
import models.registration.RegistrationSubmissionDraft
import play.api.Logging
import play.api.libs.json._
import utils.JsonOps.{doNothing, prunePath, prunePathAndPutNewValue, putNewValue}

class BackwardsCompatibilityService extends Logging {

  def adjustDraftData(draft: RegistrationSubmissionDraft): JsValue = {

    val reads = adjustAssets(draft) andThen
      adjustAgentDetails(draft)

    draft.draftData.transform(reads) match {
      case JsSuccess(updatedData, _) if updatedData != draft.draftData =>
        updatedData.transform(
          rewriteAgentDetails(updatedData) andThen
            rewriteAssets(updatedData) andThen
            pruneAgentDetails andThen
            pruneAssets
        ) match {
          case JsSuccess(adjustedData, _) =>
            adjustedData
          case JsError(errors) =>
            logger.warn(s"[Draft ID: ${draft.draftId}] Failed to rewrite data: $errors. Returning original data.")
            draft.draftData
        }
      case JsError(errors) =>
        logger.warn(s"[Draft ID: ${draft.draftId}] Failed to adjust data: $errors. Returning original data.")
        draft.draftData
      case _ =>
        logger.info(s"[Draft ID: ${draft.draftId}] Data does not need adjusting. Returning original data.")
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
    val oldPath: JsPath = __ \ "main" \ "data" \ oldKey
    draft.draftData.transform(oldPath.json.pick[T]) match {
      case JsSuccess(value, _) =>
        val newPath: JsPath = __ \ newKey
        val newObj = Json.obj(
          "_id" -> draft.draftId,
          "data" -> Json.obj(
            s"$oldKey" -> value
          ),
          "internalId" -> draft.internalId
        )
        putNewValue(newPath, newObj)
      case JsError(errors) =>
        logger.info(s"Unable to pick json at $oldPath: $errors")
        doNothing()
    }
  }

  private def rewriteAgentDetails(data: JsValue): Reads[JsObject] = {
    val path: JsPath = __ \ "agentDetails" \ "data" \ "agent"
    data.transform(path.json.pick) match {
      case JsSuccess(value, _) =>
        value.validate[AgentDetailsBC] match {
          case JsSuccess(value, _) =>
            prunePathAndPutNewValue(path, Json.toJson(value))
          case JsError(errors) =>
            logger.info(s"Unable to validate json at $path as type AgentDetailsBC: $errors")
            doNothing()
        }
      case JsError(errors) =>
        logger.info(s"Unable to pick json at $path: $errors")
        doNothing()
    }
  }

  private def rewriteAssets(data: JsValue): Reads[JsObject] = {
    val path: JsPath = __ \ "assets" \ "data" \ "assets"
    data.transform(path.json.pick[JsArray]) match {
      case JsSuccess(array, _) =>
        val newArray = array.value.zipWithIndex.foldLeft(JsArray())((acc, asset) => {
          asset._1.validate[AssetBC] match {
            case JsSuccess(value, _) => appendAssetToArray(value, acc)
            case _ => acc
          }
        })
        prunePathAndPutNewValue(path, Json.toJson(newArray))
      case JsError(errors) =>
        logger.info(s"Unable to pick json at $path: $errors")
        doNothing()
    }
  }

  private def appendAssetToArray(asset: AssetBC, array: JsArray) = {

    def append[T <: AssetBC](asset: T)(implicit wts: Writes[T]): JsArray = array :+ Json.toJson(asset)

    asset match {
      case x: MoneyAssetBC => append(x)
      case x: PropertyOrLandAssetBC => append(x)
      case x: SharesAssetBC => append(x)
      case x: BusinessAssetBC => append(x)
      case x: PartnershipAssetBC => append(x)
      case x: OtherAssetBC => append(x)
    }
  }

  private def pruneAgentDetails: Reads[JsObject] = {
    prunePath(__ \ "main" \ "data" \ "agent")
  }

  private def pruneAssets: Reads[JsObject] = {
    prunePath(__ \ "main" \ "data" \ "assets") andThen
      prunePath(__ \ "main" \ "data" \ "addAssets")
  }
}
