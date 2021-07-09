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

package controllers.transformations

import controllers.TrustsBaseController
import controllers.actions.IdentifierAction
import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.TransformationService

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationController @Inject()(
                                          identify: IdentifierAction,
                                          transformationService: TransformationService,
                                          cc: ControllerComponents
                                        ) extends TrustsBaseController(cc) with Logging {

  def removeTransforms(identifier: String): Action[AnyContent] = identify.async { request =>
    transformationService.removeAllTransformations(identifier, request.internalId) map { _ =>
      Ok
    } recoverWith {
      case _ => Future.successful(InternalServerError)
    }
  }

  def removeTrustTypeDependentTransformFields(identifier: String): Action[AnyContent] = identify.async { request =>
    transformationService.removeTrustTypeDependentTransformFields(identifier, request.internalId) map { _ =>
      Ok
    } recoverWith {
      case _ => Future.successful(InternalServerError)
    }
  }

  def removeOptionalTrustDetailTransforms(identifier: String): Action[AnyContent] = identify.async { request =>
    transformationService.removeOptionalTrustDetailTransforms(identifier, request.internalId) map { _ =>
      Ok
    } recoverWith {
      case _ => Future.successful(InternalServerError)
    }
  }

}
