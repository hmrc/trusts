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
import play.api.libs.json.{JsSuccess, JsValue, Json}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.Declaration
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.models.requests.IdentifierRequest
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
    implicit request => {

      val payload = request.body.toString()

      validator.get(config.variationsApiSchema).validate[TrustVariation](payload).fold[Future[Result]](
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
        variationRequest => doSubmit(Json.toJson(variationRequest))
      )
    }
  }

  def noChange(utr: String) = identify.async(parse.json) {
    implicit request =>
      request.body.validate[Declaration].fold(
        _ => Future.successful(BadRequest),
        declaration => {
          desService.getTrustInfo(utr, request.identifier).flatMap {
            case TrustProcessedResponse(data, _) =>
              declareNoChangeTransformer.transform(data, declaration) match {
                case JsSuccess(value, _) => doSubmit(value)
              }
          }
        }
      )
  }

  private def doSubmit(value: JsValue)(implicit request: IdentifierRequest[JsValue]) : Future[Result] = {
    desService.trustVariation(value) map { response =>

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

