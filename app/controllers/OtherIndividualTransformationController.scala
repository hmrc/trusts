/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import controllers.actions.IdentifierAction
import models.variation.NaturalPersonType
import services.OtherIndividualTransformationService
import transformers.remove.RemoveOtherIndividual
import utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class OtherIndividualTransformationController @Inject()(
                                          identify: IdentifierAction,
                                          transformService: OtherIndividualTransformationService
                                        )(implicit val executionContext: ExecutionContext,cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

    def removeOtherIndividual(identifier: String): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[RemoveOtherIndividual] match {
          case JsSuccess(otherIndividual, _) =>
            transformService.removeOtherIndividual(identifier, request.identifier, otherIndividual) map { _ =>
              Ok
            }
          case JsError(_) => Future.successful(BadRequest)
        }
      }
    }

    def amendOtherIndividual(identifier: String, index: Int): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[NaturalPersonType] match {
          case JsSuccess(otherIndividual, _) =>
            transformService.amendOtherIndividualTransformer(
              identifier,
              index,
              request.identifier,
              otherIndividual
            ) map { _ =>
              Ok
            }
          case JsError(errors) =>
            logger.warn(s"[amendOtherIndividual][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
              s"Supplied json could not be read as an Other Individual - $errors")
            Future.successful(BadRequest)
        }
      }
    }

    def addOtherIndividual(identifier: String): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[NaturalPersonType] match {
          case JsSuccess(otherIndividual, _) =>

            transformService.addOtherIndividualTransformer(
              identifier,
              request.identifier,
              otherIndividual
            ) map { _ =>
              Ok
            }
          case JsError(errors) =>
            logger.warn(s"[addOtherIndividualTransformer][Session ID: ${request.sessionId}][UTR/URN: $identifier] " +
              s"Supplied json could not be read as an Other Individual - $errors")
            Future.successful(BadRequest)
        }
      }
    }

}
