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
import transformers.AddEntityTransform
import utils.Constants.{COMPANY_TIME, COMPANY_TYPE}

case class AddSettlorTransform(entity: JsValue,
                              `type`: String) extends SettlorTransform with AddEntityTransform {

  override val trustTypeDependentFields: Seq[String] = Seq(COMPANY_TYPE, COMPANY_TIME)
}

object AddSettlorTransform {

  val key = "AddSettlorTransform"

  implicit val format: Format[AddSettlorTransform] = Json.format[AddSettlorTransform]
}
