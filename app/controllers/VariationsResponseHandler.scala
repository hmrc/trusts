/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import errors._
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc.Result
import services.auditing.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import utils.ErrorResponses._

import javax.inject.Inject

class VariationsResponseHandler @Inject()(auditService: AuditService) extends Logging {

  private val className = this.getClass.getSimpleName

  def recoverErrorResponse(variationFailure: VariationFailureForAudit, auditType: String)
                          (implicit request: IdentifierRequest[JsValue], hc: HeaderCarrier): Result = {

    variationFailure.reason match {
      case InvalidCorrelationIdErrorResponse =>
        logger.error(s"[$className][recoverErrorResponse][Session ID: ${request.sessionId}] InvalidCorrelationIdErrorResponse returned")
        auditService.auditErrorResponse(
          auditType,
          request.body,
          request.internalId,
          errorReason = "Submission has not passed validation. Invalid CorrelationId."
        )
        invalidCorrelationIdErrorResponse

      case DuplicateSubmissionErrorResponse =>
        logger.error(s"[$className][recoverErrorResponse][Session ID: ${request.sessionId}] DuplicateSubmissionErrorResponse returned")
        auditService.auditErrorResponse(auditType, request.body, request.internalId, errorReason = "Duplicate Correlation Id was submitted.")
        duplicateSubmissionErrorResponse

      case ServiceNotAvailableErrorResponse =>
        logger.error(s"[$className][recoverErrorResponse][Session ID: ${request.sessionId}] ServiceNotAvailableErrorResponse returned")
        auditService.auditErrorResponse(
          auditType,
          request.body,
          request.internalId,
          errorReason = "Service unavailable."
        )
        serviceUnavailableErrorResponse

      case EtmpCacheDataStaleErrorResponse =>
        logger.error(s"[$className][recoverErrorResponse][Session ID: ${request.sessionId}] EtmpCacheDataStaleErrorResponse returned")
        auditService.auditErrorResponse(
          auditType,
          request.body,
          request.internalId,
          errorReason = "Cached ETMP data stale."
        )
        etmpDataStaleErrorResponse

      case errorResponse =>
        logger.error(s"[$className][recoverErrorResponse][Session ID: ${request.sessionId}] " +
          s"$errorResponse returned with message: ${variationFailure.message}"
        )

        auditService.auditErrorResponse(
          auditType,
          request.body,
          request.internalId,
          errorReason = s"${variationFailure.message}"
        )

        internalServerErrorErrorResponse
    }
  }

}
