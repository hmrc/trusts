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

package transformers

import java.time.LocalDate

import play.api.libs.json._

case class RemoveSettlorsTransform(index : Int,
                                   settlorData : JsValue,
                                   endDate : LocalDate,
                                   settlorType : String
                                  ) extends DeltaTransform with JsonOperations {

  private lazy val path = __ \ "details" \ "trust" \ "entities" \ "settlors" \ settlorType

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    removeAtPosition(input, path, index)
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    endEntity(input, path, Json.toJson(settlorData), endDate)
  }
}

object RemoveSettlorsTransform {
  val key = "RemoveSettlorsTransform"

  implicit val format: Format[RemoveSettlorsTransform] = Json.format[RemoveSettlorsTransform]
}