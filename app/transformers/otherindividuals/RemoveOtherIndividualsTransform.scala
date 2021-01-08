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

package transformers.otherindividuals

import play.api.libs.json._
import transformers.{DeltaTransform, JsonOperations}

import java.time.LocalDate

case class RemoveOtherIndividualsTransform(index : Int,
                                           otherIndividualData : JsValue,
                                           endDate : LocalDate
                                  ) extends DeltaTransform with JsonOperations {

  private lazy val path = __ \ "details" \ "trust" \ "entities" \ "naturalPerson"

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    removeAtPosition(input, path, index)
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    endEntity(input, path, Json.toJson(otherIndividualData), endDate)
  }
}

object RemoveOtherIndividualsTransform {
  val key = "RemoveOtherIndividualsTransform"

  implicit val format: Format[RemoveOtherIndividualsTransform] = Json.format[RemoveOtherIndividualsTransform]
}