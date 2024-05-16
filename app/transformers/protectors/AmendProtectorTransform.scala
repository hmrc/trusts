/*
 * Copyright 2024 HM Revenue & Customs
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

package transformers.protectors

import play.api.libs.json._
import transformers.AmendEntityTransform

import java.time.LocalDate

case class AmendProtectorTransform(index: Option[Int],
                                   amended: JsValue,
                                   original: JsValue,
                                   endDate: LocalDate,
                                   `type`: String) extends ProtectorTransform with AmendEntityTransform

object AmendProtectorTransform {

  val key = "AmendProtectorTransform"

  implicit val format: Format[AmendProtectorTransform] = Json.format[AmendProtectorTransform]
}
