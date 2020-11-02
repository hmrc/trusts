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

package transformers

import java.time.LocalDate

import play.api.libs.json._
import models.variation.TrusteeOrgType

case class AmendTrusteeOrgTransform(index: Int,
                                    trustee: TrusteeOrgType,
                                    originalTrusteeJson: JsValue,
                                    override val currentDate: LocalDate
                                   ) extends DeltaTransform with AmendTrusteeCommon {

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    transform(index, input, originalTrusteeJson, Json.toJson(trustee))
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    declarationTransform(input, currentDate, index, originalTrusteeJson)
  }
}

object AmendTrusteeOrgTransform {

  val key = "AmendTrusteeOrgTransform"

  implicit val format: Format[AmendTrusteeOrgTransform] = Json.format[AmendTrusteeOrgTransform]
}