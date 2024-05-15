/*
 * Copyright 2024 HM Revenue & Customs
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
import errors.ServerError
import models.existing_trust.ExistingCheckRequest
import models.existing_trust.ExistingCheckResponse._
import models.registration.ApiResponse._
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import services.TrustsService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckTrustController @Inject()(trustsService: TrustsService,
                                     cc: ControllerComponents,
                                     identify: IdentifierAction)(implicit ec: ExecutionContext) extends TrustsBaseController(cc) with Logging {

  private val className = this.getClass.getSimpleName

  def checkExistingTrust(): Action[JsValue] = identify.async(parse.json) { implicit request =>
    withJsonBody[ExistingCheckRequest] {
      trustsCheckRequest =>
        trustsService.checkExistingTrust(trustsCheckRequest).value.map {
          case Right(Matched) => Ok(matchResponse)
          case Right(NotMatched) =>
            logger.warn(s"[$className][checkExistingTrust][Session ID: ${request.sessionId}] trust could not be matched")
            Ok(noMatchResponse)
          case Right(AlreadyRegistered) =>
          logger.warn(s"[$className][checkExistingTrust][Session ID: ${request.sessionId}] trust already registered")
          Conflict(Json.toJson(alreadyRegisteredTrustsResponse))
          case Right(response) => logger.error(s"[$className][checkExistingTrust][Session ID: ${request.sessionId}] trusts check failed due to $response")
            InternalServerError(Json.toJson(internalServerErrorResponse))
          case Left(ServerError(message)) if message.nonEmpty =>
            logger.warn(s"[$className][checkExistingTrust][Session ID: ${request.sessionId}] trusts check failed. Message: $message")
            InternalServerError(Json.toJson(internalServerErrorResponse))
          case Left(_) =>
            logger.warn(s"[$className][checkExistingTrust][Session ID: ${request.sessionId}] trusts check failed.")
            InternalServerError(Json.toJson(internalServerErrorResponse))
        }
    }
  }

}
