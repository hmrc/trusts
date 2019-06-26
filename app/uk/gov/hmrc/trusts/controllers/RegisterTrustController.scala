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
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.ApiResponse._
import uk.gov.hmrc.trusts.models.RegistrationResponse.formats
import uk.gov.hmrc.trusts.models._
import uk.gov.hmrc.trusts.services.{DesService, RosmPatternService, ValidationService}
import uk.gov.hmrc.trusts.utils.Headers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal


class RegisterTrustController @Inject()(desService: DesService, config: AppConfig,
                                        validationService: ValidationService,
                                        identify: IdentifierAction,
                                        rosmPatternService: RosmPatternService
                                        ) extends TrustsBaseController {

  def registration() = identify.async(parse.json) {
    implicit request =>

      val draftIdOption = request.headers.get(Headers.DraftRegistrationId)

      val payload = request.body.toString

      validationService
        .get(config.trustsApiRegistrationSchema)
        .validate[Registration](payload) match {

        case Right(trustsRegistrationRequest) =>

          draftIdOption match {
            case Some(draftId) =>
              desService.registerTrust(trustsRegistrationRequest).flatMap {
                case response: RegistrationTrnResponse =>
                  rosmPatternService.enrolAndLogResult(response.trn, request.affinityGroup) map {
                    _ =>
                      Ok(Json.toJson(response))
                  }
              } recover {
                case AlreadyRegisteredException =>
                  Logger.info("[RegisterTrustController][registration] Returning already registered response.")
                  Conflict(Json.toJson(alreadyRegisteredTrustsResponse))
                case NoMatchException =>
                  Logger.info("[RegisterTrustController][registration] Returning no match response.")
                  Forbidden(Json.toJson(noMatchRegistrationResponse))
                case NonFatal(e) =>
                  Logger.error(s"[RegisterTrustController][registration] Exception received : $e.")
                  InternalServerError(Json.toJson(internalServerErrorResponse))
              }
            case None =>
              Future.successful(BadRequest(Json.toJson(noDraftIdProvided)))
          }
        case Left(_) =>
          Logger.error(s"[registration] trusts validation errors, returning bad request.")
          Future.successful(invalidRequestErrorResponse)
      }
  }

}
