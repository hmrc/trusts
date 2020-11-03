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

import controllers.actions.IdentifierAction
import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import services.TrustDetailsTransformationService
import utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class TrustDetailsTransformationController @Inject()(
                                                      identify: IdentifierAction,
                                                      transformService: TrustDetailsTransformationService
                                                    )(implicit val executionContext: ExecutionContext,cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def setExpress(utr: String): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[Boolean] match {
          case JsSuccess(express, _) =>
            transformService.setExpressTransformer(
              utr,
              request.identifier,
              express
            ) map { _ =>
              Ok
            }
          case JsError(errors) =>
            logger.warn(s"[setExpress][Session ID: ${request.sessionId}] " +
              s"Supplied json could not be read as an Boolean - $errors")
            Future.successful(BadRequest)
        }
      }
    }

  def setResident(utr: String): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[Boolean] match {
          case JsSuccess(resident, _) =>
            transformService.setResidentTransformer(
              utr,
              request.identifier,
              resident
            ) map { _ =>
              Ok
            }
          case JsError(errors) =>
            logger.warn(s"[setResident][Session ID: ${request.sessionId}] " +
              s"Supplied json could not be read as an Boolean - $errors")
            Future.successful(BadRequest)
        }
      }
    }

  def setTaxable(utr: String): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[Boolean] match {
          case JsSuccess(taxable, _) =>
            transformService.setTaxableTransformer(
              utr,
              request.identifier,
              taxable
            ) map { _ =>
              Ok
            }
          case JsError(errors) =>
            logger.warn(s"[setTaxable][Session ID: ${request.sessionId}] " +
              s"Supplied json could not be read as an Boolean - $errors")
            Future.successful(BadRequest)
        }
      }
    }

  def setProperty(utr: String): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[Boolean] match {
          case JsSuccess(property, _) =>
            transformService.setTaxableTransformer(
              utr,
              request.identifier,
              property
            ) map { _ =>
              Ok
            }
          case JsError(errors) =>
            logger.warn(s"[setProperty][Session ID: ${request.sessionId}] " +
              s"Supplied json could not be read as an Boolean - $errors")
            Future.successful(BadRequest)
        }
      }
    }

  def setRecorded(utr: String): Action[JsValue] = identify.async(parse.json) {
      implicit request => {
        request.body.validate[Boolean] match {
          case JsSuccess(recorded, _) =>
            transformService.setRecordedTransformer(
              utr,
              request.identifier,
              recorded
            ) map { _ =>
              Ok
            }
          case JsError(errors) =>
            logger.warn(s"[setRecorded][Session ID: ${request.sessionId}] " +
              s"Supplied json could not be read as an Boolean - $errors")
            Future.successful(BadRequest)
        }
      }
    }

}
