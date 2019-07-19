/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.requests.IdentifierRequest
import uk.gov.hmrc.trusts.models.variation.EstateVariation
import uk.gov.hmrc.trusts.services.{AuditService, DesService, ValidationService}
import uk.gov.hmrc.trusts.utils.ErrorResponses._
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EstateVariationsController @Inject()(
                                      identify: IdentifierAction,
                                      desService: DesService,
                                      auditService: AuditService,
                                      validator: ValidationService,
                                      config : AppConfig,
                                      responseHandler: VariationsResponseHandler
                                    ) extends VariationsBaseController with ValidationUtil {

  def estateVariation() = identify.async(parse.json) {
    implicit request =>

      val payload = request.body.toString()

      validator.get(config.variationsApiSchema).validate[EstateVariation](payload).fold(
        errors => {
          Logger.error(s"[variations] estate validation errors from request body $errors.")

          auditService.auditErrorResponse(
            TrustAuditing.ESTATE_VARIATION,
            request.body,
            request.identifier,
            errorReason = "Provided request is invalid."
          )


          Future.successful(invalidRequestErrorResponse)
        },

        variationRequest => {
          desService.estateVariation(variationRequest) map { response =>

            auditService.audit(
              TrustAuditing.ESTATE_VARIATION,
              request.body,
              request.identifier,
              response = Json.toJson(response)
            )

            Ok(Json.toJson(response))

          } recover responseHandler.recoverFromException(TrustAuditing.ESTATE_VARIATION)
        }
      )
  }

}

