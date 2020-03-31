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

import play.api.libs.json._
import uk.gov.hmrc.trusts.models.variation.CharityType

case class AmendCharityBeneficiaryTransform(
                                                index: Int,
                                                amended: CharityType,
                                                original: JsValue,
                                                endDate: LocalDate
                                              )
  extends DeltaTransform
    with JsonOperations {

  private lazy val path = __ \ 'details \ 'trust \ 'entities \ 'beneficiary \ 'charity

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    amendAtPosition(input, path, index, Json.toJson(amended))
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    if (isKnownToEtmp(original)) {
      val beneficiaryWithEndDate = original.as[JsObject]
        .deepMerge(Json.obj("entityEnd" -> Json.toJson(endDate)))

      for {
        updated <- amendAtPosition(input, path, index, beneficiaryWithEndDate)
        amendedAsJson = Json.toJson(amended)
        pruned <- amendedAsJson.transform {
          (__ \ 'lineNo).json.prune andThen
            (__ \ 'bpMatchStatus).json.prune
        }
        r <- addToList(updated, path, pruned)
      } yield r

    } else {
      applyTransform(input)
    }
  }

}

object AmendCharityBeneficiaryTransform {
  val key = "AmendCharityBeneficiaryTransform"

  implicit val format: Format[AmendCharityBeneficiaryTransform] = Json.format[AmendCharityBeneficiaryTransform]
}



