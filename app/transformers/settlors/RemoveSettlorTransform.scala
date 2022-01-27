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
import transformers.RemoveEntityTransform

import java.time.LocalDate

case class RemoveSettlorTransform(index: Option[Int],
                                  entity: JsValue,
                                  endDate: LocalDate,
                                  `type`: String) extends SettlorTransform with RemoveEntityTransform

object RemoveSettlorTransform {

  val key = "RemoveSettlorTransform"

  implicit val format: Format[RemoveSettlorTransform] = Json.format[RemoveSettlorTransform]
}
