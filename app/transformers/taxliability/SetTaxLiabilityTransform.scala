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

package transformers.taxliability

import play.api.libs.json._
import transformers.{DeltaTransform, JsonOperations}

case class SetTaxLiabilityTransform(value: JsValue) extends DeltaTransform with JsonOperations {

  private lazy val path = __ \ 'yearsReturns

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    pruneThenAddTo(input, path, value)
  }

  override val isTaxableMigrationTransform: Boolean = true

}

object SetTaxLiabilityTransform {

  val key = "SetTaxLiabilityTransform"

  implicit val format: Format[SetTaxLiabilityTransform] = Json.format[SetTaxLiabilityTransform]
}
