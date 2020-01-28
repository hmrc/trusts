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

package uk.gov.hmrc.trusts.utils

import play.api.libs.json.Json
import uk.gov.hmrc.trusts.models.ErrorResponse
import play.api.mvc.Results._

object ErrorResponses {

  protected def doErrorResponse(code: String, message: String) =
    Json.toJson(ErrorResponse(code: String, message: String))

  def invalidNameErrorResponse =
    BadRequest(doErrorResponse("INVALID_NAME", "Provided name is invalid."))

  def invalidUtrErrorResponse =
    BadRequest(doErrorResponse("INVALID_UTR", "Provided utr is invalid."))

  def invalidPostcodeErrorResponse =
    BadRequest(doErrorResponse("INVALID_POSTCODE", "Provided postcode is invalid."))

  def invalidRequestErrorResponse =
    BadRequest(doErrorResponse("BAD_REQUEST", "Provided request is invalid."))

  def invalidCorrelationIdErrorResponse =
    InternalServerError(doErrorResponse("INVALID_CORRELATIONID", "Submission has not passed validation. Invalid CorrelationId."))

  def duplicateSubmissionErrorResponse =
    Conflict(doErrorResponse("DUPLICATE_SUBMISSION", "Duplicate Correlation Id was submitted."))

  def internalServerErrorErrorResponse =
    InternalServerError(doErrorResponse("INTERNAL_SERVER_ERROR", "Internal server error."))

  def serviceUnavailableErrorResponse =
    ServiceUnavailable(doErrorResponse("SERVICE_UNAVAILABLE", "Service unavailable."))

  def etmpDataStaleErrorResponse =
    BadRequest(doErrorResponse("ETMP_DATA_STALE", "ETMP returned a changed form bundle number for the trust."))
}
