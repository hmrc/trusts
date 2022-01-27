/*
 * Copyright 2022 HM Revenue & Customs
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
import utils.JsonOps.prunePathAndPutNewValue

import java.time.LocalDate

case class AmendSettlorTransform(index: Option[Int],
                                 amended: JsValue,
                                 original: JsValue,
                                 endDate: LocalDate,
                                 `type`: String) extends SettlorTransform with AmendEntityTransform {

  override val trustTypeDependentFields: Seq[String] = Seq(COMPANY_TYPE, COMPANY_TIME)

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    if (isDeceasedSettlor) {
      amendDeceasedAndPreserveData(input)
    } else {
      amendAtPosition(input, path, index, preserveFields(etmpFields))
    }
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    if (isDeceasedSettlor) {
      JsSuccess(input)
    } else {
      endSettlorIfKnownToEtmp(input)
    }
  }

  private def amendDeceasedAndPreserveData(input: JsValue): JsResult[JsObject] = {
    input.transform(
      prunePathAndPutNewValue(path, preserveFields(etmpFields :+ ENTITY_START))
    )
  }

  private def preserveFields(fields: Seq[String]): JsValue = {
    fields.foldLeft[JsValue](amended)((updated, field) => {
      copyField(original, field, updated)
    })
  }

  private def endSettlorIfKnownToEtmp(input:JsValue): JsResult[JsValue] = {
    original.transform(lineNoPick).fold(
      _ => JsSuccess(input),
      lineNo => {
        stripEtmpStatusForMatching(input, lineNo).fold(
          _ => endEntity(input, path, original, endDate, endDateField),
          newEntities => addEndedEntity(input, newEntities)
        )
      }
    )
  }

  private def stripEtmpStatusForMatching(input: JsValue, lineNo: JsValue): JsResult[Seq[JsObject]] = {
    input.transform(path.json.pick).map {
      value =>
        value.as[Seq[JsObject]].collect {
          case x: JsObject if x \ LINE_NUMBER == JsDefined(lineNo) => removeJsObjectFields(x, etmpFields)
          case x => x
        }
    }
  }

  private def addEndedEntity(input: JsValue, newEntities: Seq[JsObject]): JsResult[JsObject] = {
    input.transform(__.json.update(
      path.json.put(
        JsArray(newEntities ++ Seq(objectPlusField(original, ENTITY_END, Json.toJson(endDate))))
      )
    ))
  }
}

object AmendSettlorTransform {

  val key = "AmendSettlorTransform"

  implicit val format: Format[AmendSettlorTransform] = Json.format[AmendSettlorTransform]
}
