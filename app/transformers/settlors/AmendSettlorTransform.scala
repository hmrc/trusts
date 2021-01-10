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

package transformers.settlors

import play.api.libs.json._
import transformers.AmendEntityTransform
import utils.Constants._

import java.time.LocalDate

case class AmendSettlorTransform(index: Int,
                                 amended: JsValue,
                                 original: JsValue,
                                 endDate: LocalDate,
                                 `type`: String) extends SettlorTransform with AmendEntityTransform {

  private val isDeceasedSettlor: Boolean = `type` == DECEASED_SETTLOR

  override val path: JsPath = {
    if (isDeceasedSettlor) {
      ENTITIES \ `type`
    } else {
      ENTITIES \ SETTLORS \ `type`
    }
  }

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    if (isDeceasedSettlor) {
      for {
        lineNo <- original.transform((__ \ LINE_NUMBER).json.pick)
        bpMatchStatus <- original.transform((__ \ BP_MATCH_STATUS).json.pick)
        entityStart <- original.transform((__ \ ENTITY_START).json.pick)

        lineNoAndStatusPreserved = amended.as[JsObject] +
          (LINE_NUMBER -> lineNo) +
          (BP_MATCH_STATUS -> bpMatchStatus) +
          (ENTITY_START -> entityStart)

        trustWithAmendedDeceased <- input.transform(
          path.json.prune andThen
            __.json.update {
              path.json.put(lineNoAndStatusPreserved)
            }
        )
      } yield {
        trustWithAmendedDeceased
      }
    } else {
      amendAtPosition(input, path, index, Json.toJson(preserveEtmpStatus(amended, original)))
    }
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    if (isDeceasedSettlor) {
      JsSuccess(input)
    } else {
      original.transform(lineNoPick).fold(
        _ => JsSuccess(input),
        lineNo => {
          stripEtmpStatusForMatching(input, lineNo).fold(
            _ => endEntity(input, path, original, endDate, endDateField),
            newEntries => addEndedEntity(input, newEntries)
          )
        }
      )
    }
  }

  private def preserveEtmpStatus(amended: JsValue, original: JsValue): JsValue = {
    copyField(original, LINE_NUMBER, copyField(original, BP_MATCH_STATUS, amended))
  }

  private def stripEtmpStatusForMatching(input: JsValue, lineNo: JsValue): JsResult[Seq[JsObject]] = {
    input.transform(path.json.pick).map {
      value =>
        value.as[Seq[JsObject]].collect {
          case x: JsObject if x \ LINE_NUMBER == JsDefined(lineNo) => x - LINE_NUMBER - BP_MATCH_STATUS
          case x => x
        }
    }
  }

  private def addEndedEntity(input: JsValue, newEntities: Seq[JsObject]): JsResult[JsObject] = {
    input.transform(__.json.update(
      path.json.put(
        JsArray(newEntities ++ Seq(objectPlusField(original, "entityEnd", Json.toJson(endDate))))
      )
    ))
  }
}

object AmendSettlorTransform {

  val key = "AmendSettlorTransform"

  implicit val format: Format[AmendSettlorTransform] = Json.format[AmendSettlorTransform]
}
