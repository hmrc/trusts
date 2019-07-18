/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.controllers.actions.IdentifierAction
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.variation.EstateVariation
import uk.gov.hmrc.trusts.services.{AuditService, DesService}
import uk.gov.hmrc.trusts.utils.ErrorResponses._
import uk.gov.hmrc.trusts.utils.ValidationUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EstateVariationsController @Inject()(
                                      identify: IdentifierAction,
                                      desService: DesService,
                                      auditService: AuditService
                                    ) extends EstateBaseController with ValidationUtil {

  def estateVariation() = identify.async(parse.json) {
    implicit request =>

      request.body.validate[EstateVariation].fold(
        errors => {
          Logger.error(s"[variations] estate validation errors from request body $errors.")

          auditService.auditErrorResponse(
            TrustAuditing.ESTATE_VARIATION,
            request.body,
            request.identifier,
            errorReason = "Provided request is invalid."
          )


          Future.successful(invalidRequestErrorResponse)
        },

        variationRequest => {
          desService.estateVariation(variationRequest) map { response =>

            auditService.audit(
              TrustAuditing.ESTATE_VARIATION,
              request.body,
              request.identifier,
              response = Json.toJson(response)
            )

            Ok(Json.toJson(response))

          } recover {

            case InvalidCorrelationIdException =>

              auditService.auditErrorResponse(
                TrustAuditing.ESTATE_VARIATION,
                request.body,
                request.identifier,
                errorReason = "Submission has not passed validation. Invalid CorrelationId."
              )

              invalidCorrelationIdErrorResponse

            case DuplicateSubmissionException =>

              auditService.auditErrorResponse(
                TrustAuditing.ESTATE_VARIATION,
                request.body,
                request.identifier,
                errorReason = "Duplicate Correlation Id was submitted."
              )

              duplicateSubmissionErrorResponse

            case ServiceNotAvailableException(_) =>

              auditService.auditErrorResponse(
                TrustAuditing.ESTATE_VARIATION,
                request.body,
                request.identifier,
                errorReason = "Service unavailable."
              )

              serviceUnavailableErrorResponse

            case _ =>

              auditService.auditErrorResponse(
                TrustAuditing.ESTATE_VARIATION,
                request.body,
                request.identifier,
                errorReason = "Internal server error."
              )

              internalServerErrorErrorResponse
          }
        }
      )

  }
}

