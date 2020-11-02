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

package transformers.mdtp.beneficiaries

import play.api.libs.json._
import models.variation.UnidentifiedType

object ClassOfBeneficiaries {

  private val path = JsPath \ 'details \ 'trust \ 'entities \ 'beneficiary \ 'unidentified

  def transform(response : JsValue) : Reads[JsObject] = {
    response.transform(path.json.pick).fold(
      _ => {
        JsPath.json.update(
          path.json.put(JsArray())
        )
      },
      beneficiaries => {

        val beneficiariesUpdated = JsArray(beneficiaries.as[List[UnidentifiedType]].map {
          beneficiary =>
            Json.toJson(beneficiary)(UnidentifiedType.writeToMaintain)
        })

        JsPath.json.update(
          path.json.prune andThen
            path.json.put(beneficiariesUpdated)
        )
      }
    )
  }

}