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

package uk.gov.hmrc.trusts.models.get_trust.get_trust

import play.api.libs.json._
import uk.gov.hmrc.trusts.models.Taxability
import uk.gov.hmrc.trusts.models.Taxability.{ConvertedFromNonTaxableToTaxable, NonTaxable, Taxable}
import uk.gov.hmrc.trusts.transformers.mdtp.beneficiaries.Beneficiaries
import uk.gov.hmrc.trusts.transformers.mdtp.protectors.Protectors
import uk.gov.hmrc.trusts.transformers.mdtp.settlors.Settlors
import uk.gov.hmrc.trusts.transformers.mdtp.{OtherIndividuals, Trustees}

case class TrustProcessedResponse(getTrust: JsValue, responseHeader: ResponseHeader) extends GetTrustSuccessResponse {

  def transform: JsResult[TrustProcessedResponse] = {
    getTrust.transform(
      Trustees.transform(getTrust) andThen
        Beneficiaries.transform(getTrust) andThen
        Settlors.transform(getTrust) andThen
        Protectors.transform(getTrust) andThen
        OtherIndividuals.transform(getTrust)
    ).map {
      json =>
        TrustProcessedResponse(json, responseHeader)
    }
  }

  def taxability: Taxability = {
    val matchDataPath: JsPath = JsPath \ 'matchData
    val utrPath: JsPath = matchDataPath \ 'utr
    val urnPath: JsPath = matchDataPath \ 'urn

    (getTrust.transform(utrPath.json.pick).isSuccess, getTrust.transform(urnPath.json.pick).isSuccess) match {
      case (true, false) => Taxable
      case (false, true) => NonTaxable
      case (true, true) => ConvertedFromNonTaxableToTaxable
    }
  }

}

object TrustProcessedResponse {
  val mongoWrites: Writes[TrustProcessedResponse] = new Writes[TrustProcessedResponse] {
    override def writes(o: TrustProcessedResponse): JsValue = Json.obj(
      "responseHeader" -> Json.toJson(o.responseHeader)(ResponseHeader.mongoWrites),
      "trustOrEstateDisplay" -> o.getTrust)
  }
}

case class TrustFoundResponse(responseHeader: ResponseHeader) extends GetTrustSuccessResponse