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

package transformers.trustees

import play.api.libs.functional.syntax._
import play.api.libs.json._
import transformers.AmendEntityTransform
import utils.Constants._

import java.time.LocalDate

case class AmendTrusteeTransform(index: Option[Int],
                                 amended: JsValue,
                                 original: JsValue,
                                 endDate: LocalDate,
                                 `type`: String) extends TrusteeTransform with AmendEntityTransform {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    if (isLeadTrustee) {
      setLeadTrustee(input)
    } else {
      removeAndAdd(input)
    }
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    if (isLeadTrustee) {
      JsSuccess(input)
    } else {
      removeTrusteeTransform.applyDeclarationTransform(input)
    }
  }

  private val removeTrusteeTransform: RemoveTrusteeTransform = {
    RemoveTrusteeTransform(index, original, endDate, `type`)
  }

  private def setLeadTrustee(input: JsValue): JsResult[JsValue] = {
    val entityStartPath = leadTrusteePath \ ENTITY_START

    input.transform(entityStartPath.json.pick) match {
      case JsSuccess(entityStart, _) =>
        input.transform(
          leadTrusteePath.json.prune andThen
            __.json.update(leadTrusteePath.json.put(amended)) andThen
            __.json.update(entityStartPath.json.put(entityStart)) andThen
            (leadTrusteePath \ LINE_NUMBER).json.prune andThen
            (leadTrusteePath \ BP_MATCH_STATUS).json.prune
        )
      case e: JsError => e
    }
  }

  private def removeAndAdd(input: JsValue): JsResult[JsValue] = {
    for {
      startDate <- original.transform((__ \ `type` \ ENTITY_START).json.pick)
      trusteeRemovedJson <- removeTrusteeTransform.applyTransform(input)
      trusteeWithStartDate = objectPlusField(amended, ENTITY_START, startDate)
      trusteeAddedJson <- AddTrusteeTransform(trusteeWithStartDate, `type`).applyTransform(trusteeRemovedJson)
    } yield trusteeAddedJson
  }
}

object AmendTrusteeTransform {

  val key = "AmendTrusteeTransform"

  implicit val format: Format[AmendTrusteeTransform] = Json.format[AmendTrusteeTransform]

  // TODO - remove code once deployed and users no longer using old transforms
  def leadReads[T](`type`: String)(implicit rds: Reads[T], wts: Writes[T]): Reads[AmendTrusteeTransform] =
    (__ \ "leadTrustee").read[T].map {
      entity => AmendTrusteeTransform(None, Json.toJson(entity), Json.obj(), LocalDate.now(), `type`)
    }

  // TODO - remove code once deployed and users no longer using old transforms
  def nonLeadReads[T](`type`: String, amendmentField: String)(implicit rds: Reads[T], wts: Writes[T]): Reads[AmendTrusteeTransform] =
    ((__ \ "index").read[Int] and
      (__ \ amendmentField).read[T] and
      (__ \ "originalTrusteeJson").read[JsValue] and
      (__ \ "currentDate").read[LocalDate]).tupled.map {
      case (index, amended, original, endDate) =>
        AmendTrusteeTransform(Some(index), Json.toJson(amended), original, endDate, `type`)
    }
}
