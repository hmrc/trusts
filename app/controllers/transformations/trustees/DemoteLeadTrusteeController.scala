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

package controllers.transformations.trustees

import controllers.actions.IdentifierAction
import controllers.transformations.AddTransformationController
import models.requests.IdentifierRequest
import models.variation._
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, ControllerComponents, Result}
import services.{LocalDateService, TransformationService}
import transformers.DeltaTransform
import transformers.trustees.PromoteTrusteeTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DemoteLeadTrusteeController @Inject()(identify: IdentifierAction,
                                            transformationService: TransformationService,
                                            localDateService: LocalDateService)
                                           (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AddTransformationController(identify, transformationService) with TrusteeController {

  def demote(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      (validate[LeadTrusteeIndType], validate[LeadTrusteeOrgType]) match {
        case (Some(_), _) =>
          demoteIndividual(identifier)
        case (_, Some(_)) =>
          demoteBusiness(identifier)
        case _ =>
          logger.error(s"[add][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a lead trustee")
          Future.successful(BadRequest)
      }
    }
  }

  def demoteIndividual(identifier: String)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[LeadTrusteeIndType](identifier, INDIVIDUAL_LEAD_TRUSTEE).apply(request)

  def demoteBusiness(identifier: String)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[LeadTrusteeOrgType](identifier, BUSINESS_LEAD_TRUSTEE).apply(request)

  override def transform[T](value: T, `type`: String, isTaxable: Boolean)(implicit wts: Writes[T]): DeltaTransform = {
    PromoteTrusteeTransform(None, Json.toJson(value), Json.obj(), localDateService.now, `type`, isTaxable)
  }

}
