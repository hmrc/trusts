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
import uk.gov.hmrc.trusts.models.variation._
import uk.gov.hmrc.trusts.services.{LocalDateService, TrusteeTransformationService}
import uk.gov.hmrc.trusts.transformers.remove.RemoveTrustee
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class TrusteeTransformationController @Inject()(
                                          identify: IdentifierAction,
                                          trusteeTransformationService: TrusteeTransformationService,
                                          localDateService: LocalDateService
                                        )(implicit val executionContext: ExecutionContext,
                                          cc: ControllerComponents)
  extends TrustsBaseController(cc) with ValidationUtil with Logging {

  def amendLeadTrustee(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      val result = request.body.validate[AmendedLeadTrusteeIndType].map {
        leadTrustee =>
          trusteeTransformationService.addAmendLeadTrusteeIndTransformer(utr, request.identifier, leadTrustee)
      }.orElse {
        request.body.validate[AmendedLeadTrusteeOrgType].map {
          leadTrustee =>
            trusteeTransformationService.addAmendLeadTrusteeOrgTransformer(utr, request.identifier, leadTrustee)
        }
      }

      result match {
        case JsSuccess(_,_) => Future.successful(Ok)
        case JsError(errors) =>
          logger.warn(s"[amendLeadTrustee][Session ID: ${request.sessionId}]" +
            s" Supplied Lead trustee could not be read as amended lead trustee - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def removeTrustee(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveTrustee] match {
        case JsSuccess(model, _) =>
          trusteeTransformationService.addRemoveTrusteeTransformer(utr, request.identifier, model) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[removeTrustee][Session ID: ${request.sessionId}]" +
            s" Supplied RemoveTrustee could not be read as RemoveTrustee - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def addTrustee(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      val trusteeInd = request.body.validateOpt[TrusteeIndividualType].getOrElse(None)
      val trusteeOrg = request.body.validateOpt[TrusteeOrgType].getOrElse(None)

      (trusteeInd, trusteeOrg) match {
        case (Some(ind), _) =>
          trusteeTransformationService.addAddTrusteeTransformer(utr, request.identifier, TrusteeType(Some(ind), None)) map { _ =>
            Ok
          }
        case (_, Some(org)) =>
          trusteeTransformationService.addAddTrusteeTransformer(utr, request.identifier, TrusteeType(None, Some(org))) map { _ =>
            Ok
          }
        case _ =>
          logger.error(s"[addTrustee][Session ID: ${request.sessionId}]" +
            " Supplied json could not be read as an individual or organisation trustee")
          Future.successful(BadRequest)
      }
    }
  }

  def amendTrustee(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      val trusteeInd = request.body.validateOpt[TrusteeIndividualType].getOrElse(None)
      val trusteeOrg = request.body.validateOpt[TrusteeOrgType].getOrElse(None)

      (trusteeInd, trusteeOrg) match {
        case (Some(ind), _) =>
          trusteeTransformationService.addAmendTrusteeTransformer(utr, index, request.identifier, TrusteeType(Some(ind), None)) map { _ =>
            Ok
          }
        case (_, Some(org)) =>
          trusteeTransformationService.addAmendTrusteeTransformer(utr, index, request.identifier, TrusteeType(None, Some(org))) map { _ =>
            Ok
          }
        case _ =>
          logger.error(s"[amendTrustee][Session ID: ${request.sessionId}]" +
            " Supplied json could not be read as an individual or organisation trustee")
          Future.successful(BadRequest)
      }
    }
  }

  def promoteTrustee(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      val leadTrusteeInd = request.body.validateOpt[AmendedLeadTrusteeIndType].getOrElse(None)
      val leadTrusteeOrg = request.body.validateOpt[AmendedLeadTrusteeOrgType].getOrElse(None)

      (leadTrusteeInd, leadTrusteeOrg) match {
        case (Some(ind), _) =>
          trusteeTransformationService.addPromoteTrusteeIndTransformer(
            utr,
            request.identifier,
            index,
            ind,
            localDateService.now
          ).map(_ => Ok)
        case (_, Some(org)) =>
          trusteeTransformationService.addPromoteTrusteeOrgTransformer(
            utr,
            request.identifier,
            index,
            org,
            localDateService.now
          ).map(_ => Ok)
        case _ =>
          logger.error(s"[promoteTrustee][Session ID: ${request.sessionId}]" +
            " Supplied json could not be read as an individual or organisation lead trustee")
          Future.successful(BadRequest)
      }
    }
  }
}
