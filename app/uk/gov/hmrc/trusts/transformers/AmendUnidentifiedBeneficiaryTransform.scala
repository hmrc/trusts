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

case class AmendUnidentifiedBeneficiaryTransform(index: Int,
                                                 description: String,
                                                 original: JsValue,
                                                 endDate: LocalDate) extends AmendEntityTransform {

  override val path: JsPath = __ \ 'details \ 'trust \ 'entities \ 'beneficiary \ 'unidentified

  override val amended: JsValue = original.as[JsObject]
    .deepMerge(Json.obj("description" -> description)) - "lineNo" - "bpMatchStatus"
}

object AmendUnidentifiedBeneficiaryTransform {

  val key = "AmendUnidentifiedBeneficiaryTransform"

  implicit val format: Format[AmendUnidentifiedBeneficiaryTransform] = Json.format[AmendUnidentifiedBeneficiaryTransform]
}
