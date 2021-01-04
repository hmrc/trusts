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

import play.api.libs.json._
import models.variation.OtherType

case class AddOtherBeneficiaryTransform(newBeneficiary: OtherType)
  extends DeltaTransform
  with JsonOperations {

  private lazy val path = __ \ 'details \ 'trust \ 'entities \ 'beneficiary \ 'other

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    addToList(input, path, Json.toJson(newBeneficiary))
  }
}

object AddOtherBeneficiaryTransform {

  val key = "AddOtherBeneficiaryTransform"

  implicit val format: Format[AddOtherBeneficiaryTransform] = Json.format[AddOtherBeneficiaryTransform]
}
