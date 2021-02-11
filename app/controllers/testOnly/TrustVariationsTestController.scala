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

package controllers.testOnly

import config.AppConfig
import controllers._
import controllers.actions.IdentifierAction
import models.auditing.TrustAuditing
import models.variation.TrustVariation
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import services._
import utils.ErrorResponses._
import utils.ValidationUtil

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrustVariationsTestController @Inject()(
                                               identify: IdentifierAction,
                                               trustsService: TrustsService,
                                               auditService: AuditService,
                                               validator: ValidationService,
                                               config : AppConfig,
                                               responseHandler: VariationsResponseHandler,
                                               trustsStoreService: TrustsStoreService,
                                               cc: ControllerComponents
                                             ) extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def trustVariation(): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      val payload = request.body.toString()

      trustsStoreService.is5mldEnabled().flatMap {
        enabled =>
          val schema = if (enabled) config.variationsApiSchema5MLD else config.variationsApiSchema4MLD

          validator.get(schema).validate[TrustVariation](payload).fold[Future[Result]](
            errors => {
              logger.error(s"[variations] trusts validation errors from request body $errors.")

              auditService.auditErrorResponse(
                TrustAuditing.TRUST_VARIATION,
                request.body,
                request.internalId,
                errorReason = "Provided request is invalid."
              )

              Future.successful(invalidRequestErrorResponse)
            },
            variationRequest => {
              trustsService.trustVariation(Json.toJson(variationRequest)) map { response =>

                auditService.audit(
                  TrustAuditing.TRUST_VARIATION,
                  Json.toJson(variationRequest),
                  request.internalId,
                  Json.toJson(response)
                )

                Ok(Json.toJson(response))

              } recover responseHandler.recoverFromException(TrustAuditing.TRUST_VARIATION)
            }
          )
      }
    }
  }
}
