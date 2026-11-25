/*
 * Copyright 2026 HM Revenue & Customs
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

package transformers.mdtp

import models.variation.{TrusteeIndividualType, TrusteeOrgType, TrusteeType}
import play.api.libs.json._
import utils.Constants._
import utils.JsonOps.{prunePathAndPutNewValue, putNewValue}

object Trustees extends Entities[TrusteeType] {

  override val path: JsPath = ENTITIES \ TRUSTEES

  override def transform(response: JsValue)(implicit rds: Reads[TrusteeType]): Reads[JsObject] =
    response
      .transform(path.json.pick)
      .fold(
        _ => putNewValue(path, JsArray()),
        entities => {

          val reshapedForJsArray: List[TrusteeType] = entities.as[List[TrusteeType]].flatMap { x =>
            if (x.trusteeOrg.nonEmpty && x.trusteeInd.nonEmpty) {
              List(TrusteeType(x.trusteeInd, None), TrusteeType(None, x.trusteeOrg))
            } else List(x)
          }

          val updatedEntities = JsArray(reshapedForJsArray.map { entity =>
            updateEntity(entity)
          })

          prunePathAndPutNewValue(path, updatedEntities)
        }
      )

  override def updateEntity(trustee: TrusteeType): JsValue =
    trustee match {
      case TrusteeType(Some(trusteeInd), None) =>
        Json.obj(
          INDIVIDUAL_TRUSTEE -> Json.toJson(trusteeInd)(TrusteeIndividualType.writeToMaintain)
        )
      case TrusteeType(None, Some(trusteeOrg)) =>
        Json.obj(
          BUSINESS_TRUSTEE -> Json.toJson(trusteeOrg)(TrusteeOrgType.writeToMaintain)
        )
      case _                                   =>
        Json.obj()
    }

}
