/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.transformers

import java.time.LocalDate

import play.api.libs.json.{JsArray, JsDefined, JsNull, JsObject, JsPath, JsResult, JsSuccess, JsValue, Json, __}

trait AmendSettlorTransform extends DeltaTransform
  with JsonOperations {
  val index: Int
  val amended: JsValue
  val original: JsValue
  val endDate: LocalDate
  val path: JsPath

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    amendAtPosition(input, path, index, Json.toJson(preserveEtmpStatus(amended, original)))
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    original.transform(lineNoPick).fold(
      _ => JsSuccess(input),
      lineNo => {
        stripEtmpStatusForMatching(input, lineNo).fold(
          _ => endEntity(input, path, original, endDate),
          newEntries => addEndedEntity(input, newEntries)
        )
      }
    )
  }

  private def addEndedEntity(input: JsValue, newEntities: Seq[JsObject]) = {
    input.transform(__.json.update(
      path.json.put(
        JsArray(newEntities ++ Seq(objectPlusField(original, "entityEnd", Json.toJson(endDate))))
      )
    ))
  }

  private def stripEtmpStatusForMatching(input: JsValue, lineNo: JsValue) = {
    input.transform(path.json.pick).map {
      value =>
        value.as[Seq[JsObject]].collect {
          case x: JsObject if (x \ "lineNo" == JsDefined(lineNo)) => x - "lineNo" - "bpMatchStatus"
          case x => x
        }
    }
  }

  private def preserveEtmpStatus(amended: JsValue, original: JsValue) : JsValue =
    copyField(original, "lineNo",
      copyField(original, "bpMatchStatus", amended)
    )
}
