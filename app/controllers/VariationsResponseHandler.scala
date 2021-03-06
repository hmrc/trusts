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

package controllers

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import exceptions.{DuplicateSubmissionException, EtmpCacheDataStaleException, InvalidCorrelationIdException, ServiceNotAvailableException}
import models.requests.IdentifierRequest
import services.AuditService
import utils.ErrorResponses._

class VariationsResponseHandler @Inject()(auditService: AuditService) extends Logging {

  def recoverFromException(auditType: String)(implicit request: IdentifierRequest[JsValue],hc: HeaderCarrier): PartialFunction[Throwable, Result] = {

    case InvalidCorrelationIdException =>
      logger.error(s"[Session ID: ${request.sessionId}] InvalidCorrelationIdException returned")
      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.internalId,
        errorReason = "Submission has not passed validation. Invalid CorrelationId."
      )
      invalidCorrelationIdErrorResponse

    case DuplicateSubmissionException =>
      logger.error(s"[Session ID: ${request.sessionId}] DuplicateSubmissionException returned")
      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.internalId,
        errorReason = "Duplicate Correlation Id was submitted."
      )
      duplicateSubmissionErrorResponse

    case ServiceNotAvailableException(_) =>
      logger.error(s"[Session ID: ${request.sessionId}] ServiceNotAvailableException returned")
      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.internalId,
        errorReason = "Service unavailable."
      )
      serviceUnavailableErrorResponse

    case EtmpCacheDataStaleException =>
      logger.error(s"[Session ID: ${request.sessionId}] EtmpCacheDataStaleException returned")
      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.internalId,
        errorReason = "Cached ETMP data stale."
      )
      etmpDataStaleErrorResponse

    case e =>
      logger.error(s"[Session ID: ${request.sessionId}] Exception returned ${e.getMessage}")
      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.internalId,
        errorReason = s"${e.getMessage}"
      )
      internalServerErrorErrorResponse
  }


}
