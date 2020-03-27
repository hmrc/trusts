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

package uk.gov.hmrc.trusts.transformers
import java.time.LocalDate

import play.api.data.validation.ValidationError
import play.api.libs.json.{Format, JsArray, JsObject, JsResult, JsSuccess, JsValue, Json, Reads, Writes, __}
import play.api.libs.functional.syntax._

case class RemoveBeneficiariesTransform(
                                         index : Int,
                                         beneficiaryData : JsObject,
                                         endDate : LocalDate,
                                         beneficiaryType : String
                                       )
  extends DeltaTransform
  with JsonOperations {

  private lazy val path = (__ \ "details" \ "trust" \
    "entities" \ "beneficiary" \ beneficiaryType )

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    val removeFromArray = __.json.pick[JsArray].map { arr =>
        JsArray(
          arr.value.zipWithIndex.filterNot(_._2 == index).map(_._1)
        )
    }

    val xform = path.json.update(removeFromArray)

    input.transform(xform)
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    if (isKnownToEtmp(beneficiaryData)) {
      val newBeneficiary = beneficiaryData.deepMerge(Json.obj("entityEnd" -> endDate))
      val appendToArray = __.json.pick[JsArray].map (_.append(newBeneficiary))
      val xform = path.json.update(appendToArray)
      input.transform(xform)
    } else {
      JsSuccess(input)
    }
  }
}

object RemoveBeneficiariesTransform {
  val key = "RemoveBeneficiariesTransform"

  implicit val format: Format[RemoveBeneficiariesTransform] = Json.format[RemoveBeneficiariesTransform]
}