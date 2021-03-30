/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import controllers.actions.IdentifierAction
import models.DeclarationForApi
import models.auditing.TrustAuditing
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import services.{AuditService, VariationService}
import utils.ValidationUtil

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrustVariationsController @Inject()(
                                           identify: IdentifierAction,
                                           auditService: AuditService,
                                           variationService: VariationService,
                                           responseHandler: VariationsResponseHandler,
                                           cc: ControllerComponents
                                         ) extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def declare(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[DeclarationForApi].fold(
        errors => {

          auditService.audit(
            TrustAuditing.TRUST_VARIATION,
            Json.obj("declaration" -> request.body),
            request.internalId,
            Json.toJson(Json.obj())
          )

          logger.error(s"[declare][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" unable to parse json as DeclarationForApi, $errors")
          Future.successful(BadRequest)
        },
        declarationForApi => {
          variationService
            .submitDeclaration(identifier, request.internalId, declarationForApi)
            .map(response => Ok(Json.toJson(response)))
        } recover responseHandler.recoverFromException(TrustAuditing.TRUST_VARIATION_SUBMISSION_FAILED)
      )
    }
  }
}
