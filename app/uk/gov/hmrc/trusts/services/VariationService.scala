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

package uk.gov.hmrc.trusts.services

import javax.inject.Inject
import play.api.libs.json.{JsSuccess, JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.Declaration
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.models.requests.IdentifierRequest
import uk.gov.hmrc.trusts.models.variation.VariationResponse
import uk.gov.hmrc.trusts.transformers.DeclareNoChangeTransformer
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class VariationService @Inject()(desService: DesService, declareNoChangeTransformer: DeclareNoChangeTransformer, auditService: AuditService) {

  def submitDeclareNoChange(utr:String, internalId: String,  declaration: Declaration)(implicit hc:HeaderCarrier): Future[VariationResponse] = {
    desService.getTrustInfo(utr, internalId).flatMap {
      case TrustProcessedResponse(data, _) =>
        declareNoChangeTransformer.transform(data, declaration) match {
          case JsSuccess(value, _) => doSubmit(value, internalId)
        }
    }
  }

  private def doSubmit(value: JsValue, internalId: String)(implicit hc:HeaderCarrier) : Future[VariationResponse] = {
    desService.trustVariation(value) map { response =>

      auditService.audit(
        TrustAuditing.TRUST_VARIATION,
        value,
        internalId,
        Json.toJson(response)
      )

      response
    }
  }
}
