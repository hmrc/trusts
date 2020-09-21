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
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.get_trust.get_trust.{DisplayTrustSettlor, DisplayTrustSettlorCompany}
import uk.gov.hmrc.trusts.models.variation.{AmendDeceasedSettlor, Settlor, SettlorCompany}
import uk.gov.hmrc.trusts.services.SettlorTransformationService
import uk.gov.hmrc.trusts.transformers.remove.RemoveSettlor
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class SettlorTransformationController @Inject()(identify: IdentifierAction,
                                                transformService: SettlorTransformationService)
                                               (implicit val executionContext: ExecutionContext,
                                                  cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil {

  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)

  def amendIndividualSettlor(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[Settlor] match {
        case JsSuccess(settlor, _) =>

          transformService.amendIndividualSettlorTransformer(
            utr,
            index,
            request.identifier,
            settlor
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[SettlorTransformationController][amendIndividualSettlor]" +
            s" Supplied json could not be read as a Settlor - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addIndividualSettlor(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[DisplayTrustSettlor] match {
        case JsSuccess(settlor, _) =>

          transformService.addIndividualSettlorTransformer(
            utr,
            request.identifier,
            settlor
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[SettlorTransformationController][addIndividualSettlor]" +
            s" Supplied json could not be read as a Settlor - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addBusinessSettlor(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[DisplayTrustSettlorCompany] match {
        case JsSuccess(companySettlor, _) =>

          transformService.addBusinessSettlorTransformer(
            utr,
            request.identifier,
            companySettlor
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[SettlorTransformationController][addBusinessSettlor]" +
            s" Supplied json could not be read as a Settlor - $errors")
          Future.successful(BadRequest)
      }
    }
  }


  def amendBusinessSettlor(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[SettlorCompany] match {
        case JsSuccess(settlor, _) =>

          transformService.amendBusinessSettlorTransformer(
            utr,
            index,
            request.identifier,
            settlor
          ) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[SettlorTransformationController][amendBusinessSettlor]" +
            s" Supplied json could not be read as a SettlorCompany - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def removeSettlor(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveSettlor] match {
        case JsSuccess(settlor, _) =>
          transformService.removeSettlor(utr, request.identifier, settlor) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[SettlorTransformationController][removeSettlor]" +
            s" Supplied json could not be read as a RemoveSettlor - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendDeceasedSettlor(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      request.body.validate[AmendDeceasedSettlor] match {
        case JsSuccess(settlor, _) =>
          transformService.amendDeceasedSettlor(utr, request.identifier, settlor) map { _ =>
            Ok
          }
        case JsError(_) => Future.successful(BadRequest)
      }
  }

}
