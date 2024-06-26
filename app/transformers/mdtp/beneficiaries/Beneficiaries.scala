/*
 * Copyright 2024 HM Revenue & Customs
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

package transformers.mdtp.beneficiaries

import models.variation.Beneficiary
import play.api.libs.json._
import transformers.mdtp.Entities

trait Beneficiaries[T <: Beneficiary[T]] extends Entities[T]

object Beneficiaries {

  def transform(response: JsValue): Reads[JsObject] = {
    Individual.transform(response) andThen
    Company.transform(response) andThen
    Trust.transform(response) andThen
    Charity.transform(response) andThen
    ClassOfBeneficiaries.transform(response) andThen
    EmploymentRelated.transform(response) andThen
    Other.transform(response)
  }

}
