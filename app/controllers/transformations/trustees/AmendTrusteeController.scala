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

package controllers.transformations.trustees

import controllers.actions.IdentifierAction
import controllers.transformations.AmendTransformationController
import models.requests.IdentifierRequest
import models.variation._
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents, Result}
import services.TransformationService
import services.dates.LocalDateService
import transformers.DeltaTransform
import transformers.trustees.AmendTrusteeTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendTrusteeController @Inject()(identify: IdentifierAction,
                                       transformationService: TransformationService,
                                       localDateService: LocalDateService)
                                      (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AmendTransformationController(identify, transformationService) with TrusteeController {

  def amendLeadTrustee(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      (validate[AmendedLeadTrusteeIndType], validate[AmendedLeadTrusteeOrgType]) match {
        case (Some(_), _) =>
          amendLeadIndividual(identifier)
        case (_, Some(_)) =>
          amendLeadBusiness(identifier)
        case _ =>
          logger.error(s"[AmendTrusteeController][amendLeadTrustee][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a lead trustee")
          Future.successful(BadRequest)
      }
    }
  }

  def amendTrustee(identifier: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      (validate[TrusteeIndividualType], validate[TrusteeOrgType]) match {
        case (Some(_), _) =>
          amendIndividual(identifier, index)
        case (_, Some(_)) =>
          amendBusiness(identifier, index)
        case _ =>
          logger.error(s"[AmendTrusteeController][amendTrustee][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a trustee")
          Future.successful(BadRequest)
      }
    }
  }

  def amendLeadIndividual(identifier: String)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[AmendedLeadTrusteeIndType](identifier, None, INDIVIDUAL_LEAD_TRUSTEE).apply(request)

  def amendLeadBusiness(identifier: String)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[AmendedLeadTrusteeOrgType](identifier, None, BUSINESS_LEAD_TRUSTEE).apply(request)

  def amendIndividual(identifier: String, index: Int)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[TrusteeIndividualType](identifier, Some(index), INDIVIDUAL_TRUSTEE).apply(request)

  def amendBusiness(identifier: String, index: Int)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[TrusteeOrgType](identifier, Some(index), BUSINESS_TRUSTEE).apply(request)

  override def transform[T](original: JsValue, amended: T, index: Option[Int], `type`: String, isTaxable: Boolean)(implicit wts: Writes[T]): DeltaTransform = {
    AmendTrusteeTransform(index, Json.toJson(amended), original, localDateService.now, `type`)
  }

}
