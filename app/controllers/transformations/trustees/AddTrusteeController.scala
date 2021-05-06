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
import services.{TaxableMigrationService, TransformationService}
import transformers.DeltaTransform
import transformers.trustees.AddTrusteeTransform
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddTrusteeController @Inject()(identify: IdentifierAction,
                                     transformationService: TransformationService,
                                     taxableMigrationService: TaxableMigrationService)
                                    (implicit ec: ExecutionContext, cc: ControllerComponents)
  extends AddTransformationController(identify, transformationService, taxableMigrationService) with TrusteeController {

  def add(identifier: String): Action[JsValue] = identify.async(parse.json) {
    implicit request => {

      (validate[TrusteeIndividualType], validate[TrusteeOrgType]) match {
        case (Some(_), _) =>
          addIndividual(identifier)
        case (_, Some(_)) =>
          addBusiness(identifier)
        case _ =>
          logger.error(s"[add][Session ID: ${request.sessionId}][UTR/URN: $identifier]" +
            s" Supplied json could not be read as a trustee")
          Future.successful(BadRequest)
      }
    }
  }

  def addIndividual(identifier: String)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[TrusteeIndividualType](identifier, INDIVIDUAL_TRUSTEE).apply(request)

  def addBusiness(identifier: String)(implicit request: IdentifierRequest[JsValue]): Future[Result] =
    addNewTransform[TrusteeOrgType](identifier, BUSINESS_TRUSTEE).apply(request)

  override def transform[T](value: T, `type`: String, isTaxable: Boolean, migratingFromNonTaxableToTaxable: Boolean)
                           (implicit wts: Writes[T]): DeltaTransform = {
    AddTrusteeTransform(Json.toJson(value), `type`)
  }

}
