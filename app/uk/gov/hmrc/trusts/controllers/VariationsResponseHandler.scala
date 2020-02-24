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

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.exceptions.{DuplicateSubmissionException, EtmpCacheDataStaleException, InvalidCorrelationIdException, ServiceNotAvailableException}
import uk.gov.hmrc.trusts.models.requests.IdentifierRequest
import uk.gov.hmrc.trusts.services.AuditService
import uk.gov.hmrc.trusts.utils.ErrorResponses._

class VariationsResponseHandler @Inject()(auditService: AuditService) {

  def recoverFromException(auditType: String)(implicit request: IdentifierRequest[JsValue],hc: HeaderCarrier): PartialFunction[Throwable, Result] = {

    case InvalidCorrelationIdException =>
      Logger.error(s"[ErrorHandler] InvalidCorrelationIdException returned")
      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.identifier,
        errorReason = "Submission has not passed validation. Invalid CorrelationId."
      )
      invalidCorrelationIdErrorResponse

    case DuplicateSubmissionException =>
      Logger.error(s"[ErrorHandler] DuplicateSubmissionException returned")
      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.identifier,
        errorReason = "Duplicate Correlation Id was submitted."
      )
      duplicateSubmissionErrorResponse

    case ServiceNotAvailableException(_) =>
      Logger.error(s"[ErrorHandler] ServiceNotAvailableException returned")
      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.identifier,
        errorReason = "Service unavailable."
      )
      serviceUnavailableErrorResponse

    case EtmpCacheDataStaleException =>
      Logger.error(s"[ErrorHandler] EtmpCacheDataStaleException returned")
      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.identifier,
        errorReason = "Cached ETMP data stale."
      )
      etmpDataStaleErrorResponse

    case e =>
      Logger.error(s"[ErrorHandler] Exception returned ${e.getMessage}")

      auditService.auditErrorResponse(
        auditType,
        request.body,
        request.identifier,
        errorReason = s"${e.getMessage}"
      )
      internalServerErrorErrorResponse
  }


}
