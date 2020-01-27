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

package uk.gov.hmrc.trusts.controllers

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.models.variation.TrustVariation
import uk.gov.hmrc.trusts.services.{AuditService, DesService, ValidationService}
import uk.gov.hmrc.trusts.transformers.DeclareNoChangeTransformer
import uk.gov.hmrc.trusts.utils.ErrorResponses._
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrustVariationsController @Inject()(
                                           identify: IdentifierAction,
                                           desService: DesService,
                                           auditService: AuditService,
                                           validator: ValidationService,
                                           config : AppConfig,
                                           declareNoChangeTransformer: DeclareNoChangeTransformer,
                                           responseHandler: VariationsResponseHandler
                                    ) extends TrustsBaseController with ValidationUtil {

  def trustVariation() = identify.async(parse.json) {
    implicit request =>

      val payload = request.body.toString()

      validator.get(config.variationsApiSchema).validate[TrustVariation](payload).fold(
        errors => {
          Logger.error(s"[variations] trusts validation errors from request body $errors.")

          auditService.auditErrorResponse(
            TrustAuditing.TRUST_VARIATION,
            request.body,
            request.identifier,
            errorReason = "Provided request is invalid."
          )

          Future.successful(invalidRequestErrorResponse)
        },
        variationRequest => {
          desService.trustVariation(Json.toJson(variationRequest)) map { response =>

            auditService.audit(
              TrustAuditing.TRUST_VARIATION,
              Json.toJson(variationRequest),
              request.identifier,
              Json.toJson(response)
            )

            Ok(Json.toJson(response))

          } recover responseHandler.recoverFromException(TrustAuditing.TRUST_VARIATION)
        }
      )

  }

  def noChange(utr: String) = identify.async(parse.json) {
    implicit request =>
      desService.getTrustInfo(utr, request.identifier).flatMap {
        case TrustProcessedResponse(data, _) =>
          declareNoChangeTransformer.transform(data) match {
            case JsSuccess(value, _) => desService.trustVariation(value) map { response =>

              auditService.audit(
                TrustAuditing.TRUST_VARIATION,
                value,
                request.identifier,
                Json.toJson(response)
              )

              Ok(Json.toJson(response))

            } recover responseHandler.recoverFromException(TrustAuditing.TRUST_VARIATION)
          }
      }

  }
}

