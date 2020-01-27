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
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import play.mvc.Http.Response
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.trusts.services.{AuditService, DesService}
import uk.gov.hmrc.trusts.controllers.actions.{IdentifierAction, ValidateUTRAction}
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate.{BadRequestResponse, _}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{GetTrustResponse, GetTrustSuccessResponse}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class GetTrustController @Inject()(identify: IdentifierAction,
                                   auditService: AuditService,
                                   desService: DesService) extends BaseController {
  private val logger = LoggerFactory.getLogger("application." + classOf[GetTrustController].getCanonicalName)


  val errorAuditMessages: Map[GetTrustResponse, String] = Map (
    InvalidUTRResponse -> "The UTR provided is invalid.",
    InvalidRegimeResponse -> "Invalid regime received from DES.",
    BadRequestResponse -> "Bad Request received from DES.",
    NotEnoughDataResponse -> "Missing mandatory field received from DES.",
    ResourceNotFoundResponse -> "Not Found received from DES.",
    InternalServerErrorResponse -> "Internal Server Error received from DES.",
    ServiceUnavailableResponse -> "Service Unavailable received from DES."
  )

  val errorResponses: Map[GetTrustResponse, Result] = Map (
    NotEnoughDataResponse -> NoContent,
    ResourceNotFoundResponse -> NotFound
  )

  def get(utr: String): Action[AnyContent] = (ValidateUTRAction(utr) andThen identify).async {
    implicit request =>

      desService.getTrustInfo(utr, request.identifier).map {

        case response: GetTrustSuccessResponse =>

          val responseJson = Json.toJson(response)

          auditService.audit(
            event = TrustAuditing.GET_TRUST,
            request = Json.obj("utr" -> utr),
            internalId = request.identifier,
            response = responseJson
          )

          Ok(responseJson)

        case err =>
          auditService.auditErrorResponse(
            TrustAuditing.GET_TRUST,
            Json.obj("utr" -> utr),
            request.identifier,
            errorAuditMessages.getOrElse(err, "UNKNOWN")
          )
          errorResponses.getOrElse(err, InternalServerError)
      }.recover {
        case ex =>
          logger.error("Failed to get trust info", ex)
          InternalServerError
      }
  }
}
