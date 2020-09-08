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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.trusts.controllers.actions.{IdentifierAction, ValidateUtrActionProvider}
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate._
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_estate.EstateFoundResponse
import uk.gov.hmrc.trusts.services.{AuditService, DesService}

import scala.concurrent.ExecutionContext.Implicits._

@Singleton
@deprecated("API moved to it's own microservice. http://github.com/hmrc/estates", "7 September 2020")
class GetEstateController @Inject()(identify: IdentifierAction,
                                    auditService: AuditService,
                                    desService: DesService,
                                    validateUtr: ValidateUtrActionProvider,
                                    cc: ControllerComponents) extends BackendController(cc) {

  def get(utr: String): Action[AnyContent] = (validateUtr(utr) andThen identify).async {
    implicit request =>

    desService.getEstateInfo(utr) map {

      case estateFoundResponse: EstateFoundResponse =>

        val response = Json.toJson(estateFoundResponse)

        auditService.audit(
          event = TrustAuditing.GET_ESTATE,
          request = Json.obj("utr" -> utr),
          internalId = request.identifier,
          response = response
        )

        Ok(response)

      case _: InvalidUTRResponse.type =>
        auditService.auditErrorResponse(TrustAuditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, "The UTR provided is invalid.")
        InternalServerError

      case _: InvalidRegimeResponse.type =>
        auditService.auditErrorResponse(TrustAuditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, "Invalid regime received from DES.")
        InternalServerError

      case _: BadRequestResponse.type =>
        auditService.auditErrorResponse(TrustAuditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, "Bad Request received from DES.")
        InternalServerError

      case _: ResourceNotFoundResponse.type =>
        auditService.auditErrorResponse(TrustAuditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, "Not Found received from DES.")
        NotFound

      case _: InternalServerErrorResponse.type =>
        auditService.auditErrorResponse(TrustAuditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, "Internal Server Error received from DES.")
        InternalServerError

      case _: ServiceUnavailableResponse.type =>
        auditService.auditErrorResponse(TrustAuditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, "Service Unavailable received from DES.")
        InternalServerError

      case _ =>
        auditService.auditErrorResponse(TrustAuditing.GET_ESTATE, Json.obj("utr" -> utr), request.identifier, "UNKNOWN")
        InternalServerError
    }
  }
}
