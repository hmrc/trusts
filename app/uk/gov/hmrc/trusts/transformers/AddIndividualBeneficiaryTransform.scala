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

import play.api.libs.json.Reads.of
import play.api.libs.json._
import uk.gov.hmrc.trusts.models.variation.IndividualDetailsType

case class AddIndividualBeneficiaryTransform(newBeneficiary: IndividualDetailsType) extends DeltaTransform {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    val path = __ \ 'details \ 'trust \ 'entities \ 'beneficiary \ 'individualDetails

    input.transform(path.json.pick[JsArray]) match {

      case JsSuccess(value, _) =>

        if (value.value.size < 25) {
          val individualBeneficiaries: Reads[JsObject] =
            path.json.update(of[JsArray]
              .map {
                individualBeneficiaries => individualBeneficiaries :+ Json.toJson(newBeneficiary)
              }
            )
          input.transform(individualBeneficiaries)
        }
        else {
          throw new Exception("Adding an individual beneficiary would exceed the maximum allowed amount of 25")
        }
      case JsError(_) =>
        input.transform(__.json.update {
          path.json.put(JsArray(
            Seq(Json.toJson(newBeneficiary)))
          )
        })
    }
  }
}

object AddIndividualBeneficiaryTransform {

  val key = "AddIndividualBeneficiaryTransform"

  implicit val format: Format[AddIndividualBeneficiaryTransform] = Json.format[AddIndividualBeneficiaryTransform]
}

