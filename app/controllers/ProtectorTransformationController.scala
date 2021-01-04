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

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import controllers.actions.IdentifierAction
import models.variation.{Protector, ProtectorCompany}
import services.ProtectorTransformationService
import transformers.remove.RemoveProtector
import utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class ProtectorTransformationController @Inject()(identify: IdentifierAction,
                                                  transformService: ProtectorTransformationService)
                                                 (implicit val executionContext: ExecutionContext,
                                                  cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def removeProtector(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveProtector] match {
        case JsSuccess(protector, _) =>
          transformService.removeProtector(identifier, request.identifier, protector) map { _ =>
            Ok
          }
        case JsError(_) => Future.successful(BadRequest)
      }
    }
  }

  def addIndividualProtector(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[Protector] match {
        case JsSuccess(protector, _) =>

          transformService.addIndividualProtectorTransformer(
            identifier,
            request.identifier,
            protector
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addIndividualProtector][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a Protector - $errors")
          Future.successful(BadRequest)
      }
    }
  }
  def addBusinessProtector(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[ProtectorCompany] match {
        case JsSuccess(protectorCompany, _) =>
          transformService.addBusinessProtectorTransformer(
            identifier,
            request.identifier,
            protectorCompany
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[addCompanyProtector][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a ProtectorCompany - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendIndividualProtector(identifier: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[Protector] match {
        case JsSuccess(protector, _) =>
          transformService.amendIndividualProtectorTransformer(
            identifier,
            index,
            request.identifier,
            protector
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
            logger.warn(s"[amendIndividualProtector][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
              s" Supplied json could not be read as a Protector - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendBusinessProtector(identifier: String, index: Int) : Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[ProtectorCompany] match {
        case JsSuccess(businessProtector, _) =>
          transformService.amendBusinessProtectorTransformer(
            identifier,
            index,
            request.identifier,
            businessProtector
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[amendBusinessProtector][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied payload could not be read as a ProtectorCompany - $errors")
          Future.successful(BadRequest)
      }
  }
}
