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

package transformers.trustees

import models.variation.AmendedLeadTrusteeOrgType
import play.api.libs.json._
import transformers.DeltaTransform

import java.time.LocalDate

case class PromoteTrusteeOrgTransform(index: Int,
                                      newLeadTrustee: AmendedLeadTrusteeOrgType,
                                      endDate: LocalDate,
                                      originalTrusteeJson: JsValue,
                                      override val currentDate: LocalDate
                                     ) extends DeltaTransform with PromoteTrusteeCommon {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    transform(input, index, Json.toJson(newLeadTrustee), originalTrusteeJson)
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    declarationTransform(input, endDate, index, originalTrusteeJson)
  }
}

object PromoteTrusteeOrgTransform {

  val key = "PromoteTrusteeOrgTransform"

  implicit val format: Format[PromoteTrusteeOrgTransform] = Json.format[PromoteTrusteeOrgTransform]
}