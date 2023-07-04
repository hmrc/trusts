/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import controllers.actions.IdentifierAction
import errors.VariationFailureForAudit
import models.auditing.TrustAuditing
import models.variation.DeclarationForApi
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import services.VariationService
import services.auditing.AuditService
import services.nonRepudiation.NonRepudiationService
import utils.{Session, ValidationUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TrustVariationsController @Inject()(
                                           identify: IdentifierAction,
                                           auditService: AuditService,
                                           variationService: VariationService,
                                           responseHandler: VariationsResponseHandler,
                                           nonRepudiationService: NonRepudiationService,
                                           cc: ControllerComponents,
                                           appConfig: AppConfig
                                         )(implicit ec: ExecutionContext) extends TrustsBaseController(cc) with ValidationUtil with Logging {

  private val className = this.getClass.getSimpleName

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

          logger.error(s"[$className][declare][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" unable to parse json as DeclarationForApi, $errors")
          Future.successful(BadRequest)
        },
        declarationForApi => {
          variationService.submitDeclaration(identifier, request.internalId, Session.id(hc), declarationForApi).value.map {
            case Left(variationFailure: VariationFailureForAudit) =>
              responseHandler.recoverErrorResponse(variationFailure, TrustAuditing.TRUST_VARIATION_SUBMISSION_FAILED)
            case Left(_) =>
              logger.warn(s"[$className][declare][Session ID: ${request.sessionId}][UTR/URN: $identifier] failed to submit declaration")
              InternalServerError
            case Right(context) =>

              if (appConfig.nonRepudiate) {
                nonRepudiationService.maintain(identifier, context.payload)
              }
              Ok(Json.toJson(context.result))
          }
        }
      )
    }
  }
}
