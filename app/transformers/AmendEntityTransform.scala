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

import play.api.libs.json.{JsPath, JsResult, JsValue, Json}
import utils.Constants._

import java.time.LocalDate

trait AmendEntityTransform extends DeltaTransform with AddOrAmendTransform {

  val index: Option[Int]
  val amended: JsValue
  val original: JsValue
  val endDate: LocalDate
  val path: JsPath
  val endDateField: String = ENTITY_END

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    amendAtPosition(input, path, index, Json.toJson(amended))
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    for {
      inputWithEntityEnded <- endEntity(input, path, original, endDate, endDateField)
      inputWithAmendedDataForDeclaration <- prepareInputForDeclaration(inputWithEntityEnded, amended, path)
    } yield inputWithAmendedDataForDeclaration
  }

  val etmpFields: Seq[String] = Seq(LINE_NUMBER, BP_MATCH_STATUS)
}
