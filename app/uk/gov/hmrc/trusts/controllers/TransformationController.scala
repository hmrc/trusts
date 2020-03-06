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

import java.time.LocalDate

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.Action
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.models.RemoveTrustee
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustLeadTrusteeIndType, DisplayTrustLeadTrusteeOrgType, DisplayTrustLeadTrusteeType, DisplayTrustTrusteeIndividualType, DisplayTrustTrusteeOrgType, DisplayTrustTrusteeType}
import uk.gov.hmrc.trusts.services.TransformationService
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class TransformationController @Inject()(
                                          identify: IdentifierAction,
                                          transformationService: TransformationService
                                        )(implicit val executionContext: ExecutionContext) extends TrustsBaseController with ValidationUtil {
  private val logger = LoggerFactory.getLogger("application." + this.getClass.getCanonicalName)


  def amendLeadTrustee(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[DisplayTrustLeadTrusteeType] match {
        case JsSuccess(model, _) =>
          transformationService.addAmendLeadTrusteeTransformer(utr, request.identifier, model) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"Supplied Lead trustee could not be read as DisplayTrustLeadTrusteeType - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def removeTrustee(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[RemoveTrustee] match {
        case JsSuccess(model, _) =>
          transformationService.addRemoveTrusteeTransformer(utr, request.identifier, model) map { _ =>
            Ok
          }
        case JsError(_) =>
          Future.successful(BadRequest)
      }
    }
  }

  def addTrustee(utr: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      val trusteeInd = request.body.validateOpt[DisplayTrustTrusteeIndividualType].getOrElse(None)
      val trusteeOrg = request.body.validateOpt[DisplayTrustTrusteeOrgType].getOrElse(None)

      (trusteeInd, trusteeOrg) match {
        case (Some(ind), _) =>
          transformationService.addAddTrusteeTransformer(utr, request.identifier, DisplayTrustTrusteeType(Some(ind), None)) map { _ =>
            Ok
          }
        case (_, Some(org)) =>
          transformationService.addAddTrusteeTransformer(utr, request.identifier, DisplayTrustTrusteeType(None, Some(org))) map { _ =>
            Ok
          }
        case _ =>
          logger.error("[TransformationController][addTrustee] Supplied json could not be read as an individual or organisation trustee")
          Future.successful(BadRequest)
      }
    }
  }

  def promoteTrustee(utr: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      val leadTrusteeInd = request.body.validateOpt[DisplayTrustLeadTrusteeIndType].getOrElse(None)
      val leadTrusteeOrg = request.body.validateOpt[DisplayTrustLeadTrusteeOrgType].getOrElse(None)

      (leadTrusteeInd, leadTrusteeOrg) match {
        case (Some(ind), _) =>
          transformationService.addPromoteTrusteeTransformer(
            utr,
            request.identifier,
            index,
            DisplayTrustLeadTrusteeType(Some(ind), None),
            LocalDate.now()
          ).map(_ => Ok)
        case (_, Some(org)) =>
          transformationService.addPromoteTrusteeTransformer(
            utr,
            request.identifier,
            index,
            DisplayTrustLeadTrusteeType(None, Some(org)),
            LocalDate.now()
          ).map(_ => Ok)
        case _ =>
          logger.error("[TransformationController][promoteTrustee] Supplied json could not be read as an individual or organisation lead trustee")
          Future.successful(BadRequest)
      }
    }
  }
}
