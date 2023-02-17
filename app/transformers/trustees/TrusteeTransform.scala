/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, JsPath, JsSuccess, JsValue, Reads, __}
import utils.Constants._
import utils.JsonOps.doNothing

trait TrusteeTransform {
  val `type`: String
  val path: JsPath = ENTITIES \ TRUSTEES

  def isLeadTrustee: Boolean = isIndividualLeadTrustee || isBusinessLeadTrustee
  private def isIndividualLeadTrustee: Boolean = `type` == INDIVIDUAL_LEAD_TRUSTEE
  private def isBusinessLeadTrustee: Boolean = `type` == BUSINESS_LEAD_TRUSTEE

  def isIndividualTrustee: Boolean = `type` == INDIVIDUAL_TRUSTEE || isIndividualLeadTrustee

  val leadTrusteePath: JsPath = ENTITIES \ LEAD_TRUSTEE

  def putAmendedBpMatchStatus(amended: JsValue): Reads[JsObject] = {
    amended.transform((__ \ BP_MATCH_STATUS).json.pick) match {
      case JsSuccess(bpMatchStatus, _) => __.json.update((leadTrusteePath \ BP_MATCH_STATUS).json.put(bpMatchStatus))
      case _ => doNothing()
    }
  }
}
