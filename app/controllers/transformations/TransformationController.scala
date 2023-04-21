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

package controllers.transformations

import controllers.TrustsBaseController
import controllers.actions.IdentifierAction
import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.TransformationService
import javax.inject.Inject
import utils.Session

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class TransformationController @Inject()(
                                          identify: IdentifierAction,
                                          transformationService: TransformationService,
                                          cc: ControllerComponents
                                        )(implicit ec: ExecutionContext) extends TrustsBaseController(cc) with Logging {

  def removeTransforms(identifier: String): Action[AnyContent] = identify.async { implicit request =>
    transformationService.removeAllTransformations(identifier, request.internalId, Session.id(hc)).value.flatMap {
      case Left(_) => Future.successful(InternalServerError)
      case Right(_) => Future.successful(Ok)
    }
  }

  def removeTrustTypeDependentTransformFields(identifier: String): Action[AnyContent] = identify.async { implicit request =>
    transformationService.removeTrustTypeDependentTransformFields(identifier, request.internalId, Session.id(hc)).value.map {
      case Left(_) => InternalServerError
      case Right(_) => Ok
    }
  }

  def removeOptionalTrustDetailTransforms(identifier: String): Action[AnyContent] = identify.async { implicit request =>
    transformationService.removeOptionalTrustDetailTransforms(identifier, request.internalId, Session.id(hc)).value.map {
      case Left(_) => InternalServerError
      case Right(_) => Ok
    }
  }

}
