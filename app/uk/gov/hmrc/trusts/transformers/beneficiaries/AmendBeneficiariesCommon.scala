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

package uk.gov.hmrc.trusts.transformers.beneficiaries

import play.api.libs.json._

trait AmendBeneficiariesCommon {

  def getBeneficiary[T](input: JsValue, index: Int, beneficiaryType: String)
                       (implicit reads: Reads[T]): T = {

    lazy val path = __ \ 'details \ 'trust \ 'entities \ 'beneficiary \ beneficiaryType

    input.transform(path.json.pick) match {

      case JsSuccess(json, _) =>

        val list = json.as[JsArray].value.toList

        list(index).validate[T] match {
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
