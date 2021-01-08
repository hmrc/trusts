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

package transformers.beneficiaries

import play.api.libs.json._
import transformers.AmendEntityTransform
import utils.Constants.UNIDENTIFIED_BENEFICIARY

import java.time.LocalDate

case class AmendBeneficiaryTransform(override val index: Int,
                                     override val amended: JsValue,
                                     override val original: JsValue,
                                     override val endDate: LocalDate,
                                     override val `type`: String) extends BeneficiaryTransform with AmendEntityTransform {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    if (`type` == UNIDENTIFIED_BENEFICIARY) {
      val description: String = amended.as[JsString].value
      val originalAndAmendedMerged: JsObject = original.as[JsObject].deepMerge(Json.obj("description" -> description)) - "lineNo" - "bpMatchStatus"
      amendAtPosition(input, path, index, originalAndAmendedMerged)
    } else {
      super.applyTransform(input)
    }
  }
}

object AmendBeneficiaryTransform {

  val key = "AmendBeneficiaryTransform"

  implicit val format: Format[AmendBeneficiaryTransform] = Json.format[AmendBeneficiaryTransform]
}
