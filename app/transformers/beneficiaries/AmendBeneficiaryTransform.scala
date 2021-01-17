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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import transformers.AmendEntityTransform
import utils.Constants._

import java.time.LocalDate

case class AmendBeneficiaryTransform(index: Option[Int],
                                     amended: JsValue,
                                     original: JsValue,
                                     endDate: LocalDate,
                                     `type`: String) extends BeneficiaryTransform with AmendEntityTransform {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    if (`type` == UNIDENTIFIED_BENEFICIARY) {
      val description: String = amended.as[JsString].value
      val originalAndAmendedMerged: JsObject = original.as[JsObject].deepMerge(Json.obj("description" -> description))
      amendAtPosition(input, path, index, removeFields(originalAndAmendedMerged, etmpFields))
    } else {
      super.applyTransform(input)
    }
  }
}

object AmendBeneficiaryTransform {

  val key = "AmendBeneficiaryTransform"

  implicit val format: Format[AmendBeneficiaryTransform] = Json.format[AmendBeneficiaryTransform]

  // TODO - remove code once deployed and users no longer using old transforms
  def reads(`type`: String, amendmentField: String = "amended"): Reads[AmendBeneficiaryTransform] =
    ((__ \ "index").read[Int] and
      (__ \ amendmentField).read[JsValue] and
      (__ \ "original").read[JsValue] and
      (__ \ "endDate").read[LocalDate]).tupled.map {
      case (index, amended, original, endDate) =>
        AmendBeneficiaryTransform(Some(index), amended, original, endDate, `type`)
    }
}
