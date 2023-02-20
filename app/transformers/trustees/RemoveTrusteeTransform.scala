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

import play.api.libs.json._
import transformers.RemoveEntityTransform
import utils.Constants._

import java.time.LocalDate

case class RemoveTrusteeTransform(index: Option[Int],
                                  entity: JsValue,
                                  endDate: LocalDate,
                                  `type`: String) extends TrusteeTransform with RemoveEntityTransform {

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    if (isTrusteeKnownToEtmp) {
      entity.transform(putEndDate) match {
        case JsSuccess(endedTrusteeJson, _) => addToList(input, path, endedTrusteeJson)
        case e: JsError => e
      }
    } else {
      JsSuccess(input)
    }
  }

  private def isTrusteeKnownToEtmp: Boolean = {
    entity.transform((__ \ `type` \ LINE_NUMBER).json.pick).isSuccess
  }

  private def putEndDate: Reads[JsObject] = {
    val entityEndPath: JsPath = __ \ `type` \ ENTITY_END
    __.json.update(entityEndPath.json.put(Json.toJson(endDate)))
  }
}

object RemoveTrusteeTransform {

  val key = "RemoveTrusteeTransform"

  implicit val format: Format[RemoveTrusteeTransform] = Json.format[RemoveTrusteeTransform]
}
