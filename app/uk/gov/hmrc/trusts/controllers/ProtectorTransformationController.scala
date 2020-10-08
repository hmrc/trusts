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

package uk.gov.hmrc.trusts.controllers

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.RemoveProtector
import uk.gov.hmrc.trusts.models.variation.{Protector, ProtectorCompany}
import uk.gov.hmrc.trusts.services.ProtectorTransformationService
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class ProtectorTransformationController @Inject()(identify: IdentifierAction,
                                                  transformService: ProtectorTransformationService)
                                                 (implicit val executionContext: ExecutionContext,
                                                  cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def removeProtector(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveProtector] match {
        case JsSuccess(protector, _) =>
          transformService.removeProtector(utr, request.identifier, protector) map { _ =>
            Ok
          }
        case JsError(_) => Future.successful(BadRequest)
      }
    }
  }

  def addIndividualProtector(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[Protector] match {
        case JsSuccess(protector, _) =>

          transformService.addIndividualProtectorTransformer(
            utr,
            request.identifier,
            protector
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[ProtectorTransformationController][addIndividualProtector]" +
            s" Supplied json could not be read as a Protector - $errors")
          Future.successful(BadRequest)
      }
    }
  }
  def addBusinessProtector(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[ProtectorCompany] match {
        case JsSuccess(protectorCompany, _) =>
          transformService.addBusinessProtectorTransformer(
            utr,
            request.identifier,
            protectorCompany
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[ProtectorTransformationController][addCompanyProtector] Supplied json could not be read as a Company Beneficiary - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendIndividualProtector(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[Protector] match {
        case JsSuccess(protector, _) =>
          transformService.amendIndividualProtectorTransformer(
            utr,
            index,
            request.identifier,
            protector
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
            logger.warn(s"[ProtectorTransformationController][amendIndividualProtector]" +
              s" Supplied json could not be read as a Protector - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendBusinessProtector(utr: String, index: Int) : Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[ProtectorCompany] match {
        case JsSuccess(businessProtector, _) =>
          transformService.amendBusinessProtectorTransformer(
            utr,
            index,
            request.identifier,
            businessProtector
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[ProtectorTransformationController][amendBusinessProtector]" +
            s" Supplied payload could not be read as a ProtectorCompany - $errors")
          Future.successful(BadRequest)
      }
  }
}
