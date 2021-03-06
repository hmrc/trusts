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
import models.existing_trust.ExistingCheckRequest
import models.existing_trust.ExistingCheckResponse._
import models.registration.ApiResponse._
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import services.TrustsService

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

class CheckTrustController @Inject()(trustsService: TrustsService,
                                     cc: ControllerComponents,
                                     identify: IdentifierAction) extends TrustsBaseController(cc) with Logging {

  def checkExistingTrust(): Action[JsValue] = identify.async(parse.json) { implicit request =>
    withJsonBody[ExistingCheckRequest] {
      trustsCheckRequest =>
        trustsService.checkExistingTrust(trustsCheckRequest).map {
          result =>
            logger.info(s"[checkExistingTrust][Session ID: ${request.sessionId}] response: $result")
            result match {
              case Matched => Ok(matchResponse)
              case NotMatched => Ok(noMatchResponse)
              case AlreadyRegistered => Conflict(Json.toJson(alreadyRegisteredTrustsResponse))
              case _ => InternalServerError(Json.toJson(internalServerErrorResponse))
            }
        }
    }
  }

}
