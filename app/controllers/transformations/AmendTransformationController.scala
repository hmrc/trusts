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

package controllers.transformations

import controllers.TrustsBaseController
import controllers.actions.IdentifierAction
import controllers.transformations.TransformationHelper.isTrustTaxable
import errors.ServerError
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import services.TransformationService
import transformers.DeltaTransform
import utils.Session

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

abstract class AmendTransformationController @Inject()(identify: IdentifierAction,
                                                       transformationService: TransformationService)
                                                      (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends TrustsBaseController(cc) with TransformationHelper with Logging {

  private val className = this.getClass.getSimpleName

  def transform[T](original: JsValue, amended: T, index: Option[Int], `type`: String, isTaxable: Boolean)(implicit wts: Writes[T]): DeltaTransform

  def addNewTransform[T](identifier: String, index: Option[Int], `type`: String = "")(implicit rds: Reads[T], wts: Writes[T]): Action[JsValue] = {
    identify.async(parse.json) {
      implicit request => {
        request.body.validate[T] match {

          case JsSuccess(amendedEntity, _) =>
            val expectedResult = for {
              trust <- transformationService.getTransformedTrustJson(identifier, request.internalId, Session.id(hc))
              isTaxable <- isTrustTaxable(trust)
              originalEntity <- findJson(trust, `type`, index)
              _ <- transformationService.addNewTransform(identifier, request.internalId, transform(originalEntity, amendedEntity, index, `type`, isTaxable))
            } yield {
              Ok
            }

            expectedResult.value.map {
              case Right(status) => status
              case Left(ServerError(message)) if message.nonEmpty =>
                logger.warn(s"[$className][addNewTransform][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
                  s"failed to add new transform. Message: $message")
                InternalServerError
              case Left(_) =>
                logger.warn(s"[$className][addNewTransform][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
                  s"failed to add new transform.")
                InternalServerError
            }

          case JsError(errors) =>
            logger.warn(s"[$className][addNewTransform][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
              s"Supplied json did not pass validation - $errors")
            Future.successful(BadRequest)
        }
      }
    }
  }

}
