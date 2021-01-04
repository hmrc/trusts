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

package transformers

import java.time.LocalDate

import play.api.libs.json._

case class RemoveTrusteeTransform(endDate: LocalDate, index: Int, trusteeToRemove: JsValue)
  extends DeltaTransform
    with JsonOperations {

  private val trusteePath = (__ \ 'details \ 'trust \ 'entities \ 'trustees)

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    removeAtPosition(input, trusteePath, index)
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    if (trusteeIsKnownToEtmp(trusteeToRemove)) {
      trusteeToRemove.transform(addEntityEnd(trusteeToRemove, endDate)) match {
        case JsSuccess(endedTrusteeJson, _) => addToList(input, trusteePath, endedTrusteeJson)
        case e: JsError => e
      }
    } else {
      // Do not add the trustee back into the record
      super.applyDeclarationTransform(input)
    }
  }

  private def trusteeIsKnownToEtmp(json: JsValue): Boolean = {
    json.transform((__ \ 'trusteeInd \ 'lineNo).json.pick).isSuccess |
      json.transform((__ \ 'trusteeOrg \ 'lineNo).json.pick).isSuccess
  }

  private def addEntityEnd(trusteeToRemove: JsValue, endDate: LocalDate): Reads[JsObject] = {
    val entityEndPath =
      if (trusteeToRemove.transform((__ \ 'trusteeInd).json.pick).isSuccess) {
        (__ \ 'trusteeInd \ 'entityEnd).json
      } else {
        (__ \ 'trusteeOrg \ 'entityEnd).json
      }

    __.json.update(entityEndPath.put(Json.toJson(endDate)))
  }
}

object RemoveTrusteeTransform {

  val key = "RemoveTrusteeTransform"

  implicit val format: Format[RemoveTrusteeTransform] = Json.format[RemoveTrusteeTransform]
}
