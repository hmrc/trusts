/*
 * Copyright 2022 HM Revenue & Customs
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

package models.get_trust

import play.api.libs.json._
import transformers.mdtp.beneficiaries.Beneficiaries
import transformers.mdtp.protectors.Protectors
import transformers.mdtp.settlors.Settlors
import transformers.mdtp.assets.Assets
import transformers.mdtp.{OtherIndividuals, Trustees}

case class TrustProcessedResponse(getTrust: JsValue, responseHeader: ResponseHeader) extends GetTrustSuccessResponse {

  def transform: JsResult[TrustProcessedResponse] = {
    getTrust.transform(
      Trustees.transform(getTrust) andThen
        Beneficiaries.transform(getTrust) andThen
        Settlors.transform(getTrust) andThen
        Protectors.transform(getTrust) andThen
        OtherIndividuals.transform(getTrust) andThen
        Assets.transform(getTrust)
    ).map {
      json =>
        TrustProcessedResponse(json, responseHeader)
    }
  }

}

object TrustProcessedResponse {
  val mongoWrites: Writes[TrustProcessedResponse] = (o: TrustProcessedResponse) => Json.obj(
    "responseHeader" -> Json.toJson(o.responseHeader)(ResponseHeader.mongoWrites),
    "trustOrEstateDisplay" -> o.getTrust
  )
}

case class TrustFoundResponse(responseHeader: ResponseHeader) extends GetTrustSuccessResponse
