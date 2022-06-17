/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.actions.IdentifierAction
import controllers.TrustsBaseController
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import transformers.DeltaTransform
import transformers.remove.Remove
import javax.inject.Inject
import utils.Session

import scala.concurrent.{ExecutionContext, Future}

abstract class RemoveTransformationController @Inject()(identify: IdentifierAction,
                                                        transformationService: TransformationService)
                                                       (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends TrustsBaseController(cc) with TransformationHelper with Logging {

  def transform[T <: Remove](remove: T, entity: JsValue): DeltaTransform

  def addNewTransform[T <: Remove](identifier: String)(implicit rds: Reads[T]): Action[JsValue] = {
    identify.async(parse.json) {
      implicit request => {
        request.body.validate[T] match {

          case JsSuccess(remove, _) =>
            for {
              json <- transformationService.getTransformedTrustJson(identifier, request.internalId, Session.id(hc))
              entity <- Future.fromTry(findJson(json, remove.`type`, Some(remove.index)))
              _ <- transformationService.addNewTransform(identifier, request.internalId, transform(remove, entity))
            } yield {
              Ok
            }

          case JsError(errors) =>
            logger.warn(s"[AmendTransformationController][addNewTransform][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
              s"Supplied json did not pass validation - $errors")
            Future.successful(BadRequest)
        }
      }
    }
  }

}
