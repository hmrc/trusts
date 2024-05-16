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
import transformers.trustees.PromoteTrusteeTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PromoteTrusteeController @Inject()(identify: IdentifierAction,
                                         transformationService: TransformationService,
                                         localDateService: LocalDateService)
                                        (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AmendTransformationController(identify, transformationService) with TrusteeController {

  def promote(identifier: String, index: Int): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      (validate[AmendedLeadTrusteeIndType], validate[AmendedLeadTrusteeOrgType]) match {
        case (Some(_), _) =>
          promoteIndividual(identifier, index)
        case (_, Some(_)) =>
          promoteBusiness(identifier, index)
        case _ =>
          logger.error(s"[PromoteTrusteeController][promote][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as an amended lead trustee")
          Future.successful(BadRequest)
      }
    }
  }

  def promoteIndividual(identifier: String, index: Int)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[AmendedLeadTrusteeIndType](identifier, Some(index), INDIVIDUAL_TRUSTEE).apply(request)

  def promoteBusiness(identifier: String, index: Int)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[AmendedLeadTrusteeOrgType](identifier, Some(index), BUSINESS_TRUSTEE).apply(request)

  override def transform[T](original: JsValue, amended: T, index: Option[Int], `type`: String, isTaxable: Boolean)(implicit wts: Writes[T]): DeltaTransform = {
    PromoteTrusteeTransform(index, Json.toJson(amended), original, localDateService.now, `type`, isTaxable)
  }

}
