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

import play.api.libs.json._
import uk.gov.hmrc.trusts.models.variation.UnidentifiedType

case class AmendUnidentifiedBeneficiaryTransform(index: Int, description: String) extends DeltaTransform {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    val unidentifiedBeneficiaryPath = __ \ 'details \ 'trust \ 'entities \ 'beneficiary \ 'unidentified

    input.transform(unidentifiedBeneficiaryPath.json.pick) match {

      case JsSuccess(json, _) =>

        val updatedBeneficiaryDetails = getOriginalBeneficiaryDetails(input, index).copy(description = description)

        val array = json.as[JsArray]

        val updated = (array.value.take(index) :+ Json.toJson(updatedBeneficiaryDetails)) ++ array.value.drop(index + 1)

        input.transform(
          unidentifiedBeneficiaryPath.json.prune andThen
            JsPath.json.update {
              unidentifiedBeneficiaryPath.json.put(Json.toJson(updated))
            }
        )

      case e: JsError => e
    }
  }

  private def getOriginalBeneficiaryDetails(input: JsValue, index: Int): UnidentifiedType = {
    val unidentifiedBeneficiaryPath = __ \ 'details \ 'trust \ 'entities \ 'beneficiary \ 'unidentified

    input.transform(unidentifiedBeneficiaryPath.json.pick) match {

      case JsSuccess(json, _) =>

        val list = json.as[JsArray].value.toList

        list(index).validate[UnidentifiedType] match {
          case JsSuccess(value, _) =>
            value

          case JsError(errors) =>
            throw JsResultException(errors)
        }
      case JsError(errors) =>
        throw JsResultException(errors)
    }
  }
}



object AmendUnidentifiedBeneficiaryTransform {

  val key = "AmendUnidentifiedBeneficiaryTransform"

  implicit val format: Format[AmendUnidentifiedBeneficiaryTransform] = Json.format[AmendUnidentifiedBeneficiaryTransform]
}