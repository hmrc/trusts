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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import transformers.RemoveEntityTransform

import java.time.LocalDate

case class RemoveSettlorTransform(index: Option[Int],
                                  entity: JsValue,
                                  endDate: LocalDate,
                                  `type`: String) extends SettlorTransform with RemoveEntityTransform

object RemoveSettlorTransform {

  val key = "RemoveSettlorTransform"

  implicit val format: Format[RemoveSettlorTransform] = Json.format[RemoveSettlorTransform]

  // TODO - remove code once deployed and users no longer using old transforms
  def reads: Reads[RemoveSettlorTransform] =
    ((__ \ "index").read[Int] and
      (__ \ "settlorData").read[JsValue] and
      (__ \ "endDate").read[LocalDate] and
      (__ \ "settlorType").read[String]).tupled.map {
      case (index, entity, endDate, sType) =>
        RemoveSettlorTransform(Some(index), entity, endDate, sType)
    }
}
