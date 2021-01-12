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

package transformers.assets

import play.api.libs.json._
import transformers.AmendEntityTransform

import java.time.LocalDate

case class AmendAssetTransform(index: Option[Int],
                               amended: JsValue,
                               original: JsValue,
                               endDate: LocalDate,
                               `type`: String) extends AssetTransform with AmendEntityTransform {

  override val endDateField: String = nonEeaBusinessEndDateField

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    if (isNonEeaBusiness) {
      endEntity(input, path, original, endDate, endDateField)
    } else {
      JsSuccess(input)
    }
  }

  override val isTaxableMigrationTransform: Boolean = !isNonEeaBusiness
}

object AmendAssetTransform {

  val key = "AmendAssetTransform"

  implicit val format: Format[AmendAssetTransform] = Json.format[AmendAssetTransform]
}
